#!/usr/bin/env perl
# Remote PC Manager
# Raspberry Pi hosted daemon
# Dispatches everything from the web backend to the PC if necessary
use strict;
use warnings;
use IO::Socket::INET;
use IO::Select;
use File::Basename;
use File::MimeInfo::Magic;
use File::Spec;
use URI;
use URI::Escape;
use JSON;
# Remember to always enable high precision sleeping
# when using sleep with float arguments, otherwise you'll
# you'll be swearing quite a bit of time like I did...
use Time::HiRes qw(sleep);
use threads;

$|++;
my ($address, $port, $authPassword, $uri, $passwd, $captureExecPath, $captureExecParams, $relayPin, $delay);

# -- CONFIGURATION --
# Enter here the local IP address of the desidered PC.
$address = "192.168.1.15";

# Enter here the port you configured in the daemon.
$port = 18369;

# If you are using the "password" authentication type in the damon,
# please insert the plaintext password here.
$authPassword = "dummy";

# Insert here the uri of trigger_action.php of the remote server.
$uri = "http://example.com/trigger_action.php";

# Set the password you configured in the PHP pass for 'raspi', in
# plaintext
$passwd = "1234";

# Set the path of the 'capture' program to take webcam images.
# Link to the sourcecode: http://r.usr.sh/mirror/capture/capture.cpp
# Be sure to chmod +x.
# Put -1 if you don't need that feature.
$captureExecPath = File::Spec->catfile (dirname (__FILE__), "capture");

# Set the params of the 'capture' executable.
# ** NOTE ** Be sure to set %OUTPUTPATH% as the output path!
# ** NOTE ** Do not put -1 here!
$captureExecParams = "-w 1280 -h 720 -o \"%OUTPUTPATH%\" -t jpg";

# Select the relay PIN where you attached the connection cables.
$relayPin = 0; # probably you want to put 0 or 1 (by default there are only 2 relays)

# Main loop delay. I recommend leaving this to the
# default value (10s)
$delay = 10;

# -- END OF CONFIGURATION --
# Do not touch any code if you don't know what you are doing.
# Kthxbye.

my $parsedTargetUrl = URI->new ($uri);
my $hardcodedImgName = "rpm-webcam-img.jpg";
my $hardcodedFieldName = "allyourfilearebelongtous";
$captureExecParams .= " >" . File::Spec->devnull() . " 2>&1";

print "Trying to import PiFace's module\n";
eval { require PiFace; };
if ($@) {
    die ("** ERROR ** Cannot import PiFace's module. You can find it over here: https://github.com/Robertof/piface-perl (sorry, not in CPAN). TODO: make the module optional.\n");
}

PiFace::pfio_init();

print "> Remote PC Manager RPi-hosted daemon is starting...\n";
print "  PC daemon IP/port: ${address}:${port}\n";
print "  Looping every: ${delay} seconds\n";

print "> Collecting monitor information\n";
my $monitor_enumeration_data = send_message ("enumerate-monitors", 1);
my $monitor_enumeration_ok = 0;
if ($monitor_enumeration_data eq -1) {
    die ("*** ERROR *** Can't connect to ${address}:${port}. Be sure to start the daemon on the target PC!\n");
}
my $_code = parse_error_code ($monitor_enumeration_data);
if ($_code ne 0xF00D) {
    warn "Monitor enumeration failed. (server returned ${_code}). Disabling screenshot functionality...\n";
} else {
    # parse json data
    die ("Monitor enumeration failed! Server sent a wrong reply: ${monitor_enumeration_data}") if not $monitor_enumeration_data =~ /^ERRCODE=\d+&DATA=(\[.+)$/i;
    print "  Sending monitor data to the remote server\n";
    my $val = trigger_action ("action=upload_monitor_info&data=" . uri_escape ($1));
    if ($val ne "OK") {
        die ("Monitor enumeration failed, remote server status: ${val}\n");
    } else {
        $monitor_enumeration_ok = 1;
    }
}

print "> Loop is starting\n";

my $secondsElapsed = -1;
my $pc_alive;
my $tw_alive;

while (1)
{
    # handle: PC heartbeat
    if ($secondsElapsed eq -1 || $secondsElapsed >= 60)
    {
        $secondsElapsed = 0;
        print "  Sending heartbeat(s) if necessary\n";
        $pc_alive = send_message ("pc-alive") eq 0xF00D;
        if ($pc_alive) {
            $tw_alive = send_message ("tw-alive") eq 0xF00D;
            warn "Got an invalid response while keepaliving" if trigger_action ("action=ping&target=pc" . ($tw_alive ? ",teamviewer" : "")) ne "OK";
        } else { $tw_alive = 0; }
    }
    # handle: TW/PC remote status change handling
    my $thr_st  = threads->create (\&threaded_read_status);
    my $thr_act = threads->create (\&threaded_read_actions);
    my $scheduled_sc = $thr_st->join();
    if ($scheduled_sc =~ /^\{/)
    {
        my $decoded_json = decode_json $scheduled_sc;
        my $doWithPc = $decoded_json->{"pc"};
        my $doWithTw = $decoded_json->{"teamviewer"};
        $pc_alive = parse_bool ($decoded_json->{"pc_alive"});
        $tw_alive = parse_bool ($decoded_json->{"teamviewer_alive"});
        if ($doWithPc ne -1)
        {
            if ($doWithPc eq 0 and $pc_alive) {
                warn "An error occurred while shutdowning the PC" if send_message ("pc-turnoff") ne 0xF00D;
                $pc_alive = 0;
            } if ($doWithPc eq 1 and $pc_alive eq 0) {
                print "  Turning on the PC\n";
                async (sub {
                    PiFace::pfio_digital_write ($relayPin, 1);
                    sleep (0.2);
                    PiFace::pfio_digital_write ($relayPin, 0);
                })->detach; # 'cause threaded is better
            }
        }
        if ($doWithTw ne -1 and $pc_alive)
        {
            if ($doWithTw eq 0 and $tw_alive) {
                warn "An error occurred while turning off TeamViewer" if send_message ("tw-turnoff") ne 0xF00D;
                async (\&trigger_action, "action=teamviewer_immediate_shutdown")->detach;
                $tw_alive = 0;
            } elsif ($doWithTw eq 1 and not $tw_alive) {
                warn "An error occurred while turning on TeamViewer" if send_message ("tw-turnon") ne 0xF00D;
                $secondsElapsed = -$delay - 1; # force heartbeat sending
            }
        }
    } else {
        warn "Got an invalid response while reading statuses: ${scheduled_sc}";
    }
    # handle: actions (dispatches screenshot requests and takes webcam images)
    my $scheduled_act = $thr_act->join();
    if ($scheduled_act =~ /^\{/)
    {
        my $datgison = decode_json $scheduled_act;
        my ($screenshot, $webcam) = ($datgison->{"screenshot"}, $datgison->{"webcam"});
        if ($screenshot ne -2 and $monitor_enumeration_ok)
        {
            if ($pc_alive) {
                # we do not waste time by calling upload_error since it was already done
                # also we spawn a thread because the client waits to complete the upload before 
                # closing the connection
                async (sub {
                    if ((send_message ("take-screenshot" . ($screenshot eq -1 ? "ALL" : $screenshot))) != 0xF00D) {
                        warn "An error occurred while taking a screenshot";
                    }
                })->detach;
            } else {
                warn "do_screenshot called while pc is not alive";
                async (\&upload_error, "screenshot")->detach;
            }
        }
        # TODO: check if the thread died
        if ($webcam)
        {
            print "  Taking and uploading a webcam image\n";
            async (\&take_webcam_image)->detach;
        }
    }
    sleep ($delay);
    $secondsElapsed += $delay;
}

sub take_webcam_image
{
    if ($captureExecPath eq -1)
    {
        warn "Tried to take a webcam image without having the proper capture executable";
        upload_error ("webcam");
        return;
    }
    my $expected_path = getCaptureOutputPath();
    if (system (&getCaptureRunLine ($expected_path)) ne 0 or !-e $expected_path)
    {
        warn "Can't take a webcam image, capture returned a wrong exit code (or it didn't save in the expected path)";
        upload_error ("webcam");
        return;
    }
    # upload stuff now
    my $upout = send_dat_file ("${uri}?pwd=${passwd}&action=upload&context=webcam", $expected_path, $hardcodedFieldName);
    if ($upout ne -0xB00B5)
    {
        warn "An error occurred while uploading the webcam image: ${upout}";
        upload_error ("webcam");
        return;
    }
    unlink $expected_path;
    print "  Done\n";
    return;
}

sub threaded_read_status
{
    return trigger_action ("action=read_statuses");
}

sub threaded_read_actions
{
    return trigger_action ("action=read_scheduled_actions");
}

sub upload_error
{
    return trigger_action ("action=upload&context=" . $_[0] . "&emergency-error=true");
}

sub send_message
{
    my $cmd = shift;
    my $retoutput = shift || 0;
    my $srvsock = IO::Socket::INET->new (
        PeerAddr => $address,
        PeerPort => $port,
        Proto    => "tcp"
    ) || return -1;
    print $srvsock "AUTHSTRING=${authPassword}&MESSAGE=${cmd}\n";
    return -1 if not IO::Select->new ($srvsock)->can_read (6);
    my $sent = <$srvsock>;
    chomp ($sent);
    close $srvsock;
    my $code = parse_error_code ($sent);
    die ("Server sent an unknown response (${sent})") if $code eq -1;
    die ("We sent an invalid message: ${cmd}") if $code eq 0xB15B00B5 or $code eq 0xFAC64B17;
    die ("We timed out") if $code eq 0xFAC;
    die ("We failed the authentication (wrong password or ip)") if $code eq 0xDEADBEEF;
    warn "Received code which isn't 0xF00D: ${code} with ${cmd}" if $code ne 0xF00D and $code ne 0x5ADFACE;
    return $code if not $retoutput;
    return $sent;
}

sub parse_error_code
{
    return $1 if ($_[0] =~ /^ERRCODE=(\d+)/);
    return -1;
}

sub is_success
{
    parseErrCode ($_[0]) eq 0xF00D;
}

sub getCaptureOutputPath
{
    return File::Spec->catfile (dirname ($captureExecPath), $hardcodedImgName);
}

sub getCaptureRunLine
{
    my $out = shift;
    $captureExecParams =~ s/%OUTPUTPATH%/$out/gi;
    return "${captureExecPath} ${captureExecParams}";
}

sub _gen_chunked
{
    my $str = shift;
    my $length = sprintf ("%x", length ($str));
    return "${length}\r\n${str}\r\n";
}

sub send_dat_file
{
    # $filePath should exist
    my ($uri, $filePath, $fileField) = @_;
    my $boundary = "---a0b1ef871459163ex"; # TODO: randomization of the boundary
    open OMNOM, "<", $filePath or return &{sub {
        warn "Can't open ${filePath}: ${!} (${@})\n";
        return "Can't open the file";
    }}();
    binmode OMNOM;
    my $uriobject = URI->new ($uri);
    my $sock = IO::Socket::INET->new (
        PeerAddr => $uriobject->host,
        PeerPort => $uriobject->port,
        Proto    => "tcp"
    );
    if (not defined $sock or not $sock)
    {
        warn "Can't connect to " . $uriobject->host . ": ${!}. Skipping\n";
        close OMNOM;
        return "Connection failed";
    }
    binmode $sock;
    print $sock "POST " . $uriobject->path_query . " HTTP/1.1\r\n";
    print $sock "Host: " . $uriobject->host . "\r\n";
    print $sock "Transfer-Encoding: chunked\r\n";
    print $sock "Connection: close\r\n";
    print $sock "Content-Type: multipart/form-data; boundary=${boundary}\r\n\r\n";
    # chunked transfer begins now
    print $sock _gen_chunked ("--${boundary}\r\nContent-Disposition: form-data; name=\"${fileField}\"; filename=\"" . basename ($filePath) . "\"\r\nContent-Type: " . mimetype ($filePath) . "\r\n\r\n");
    my ($buffer, $readbytes);
    while (($readbytes = read (OMNOM, $buffer, 4096)) > 0)
    {
        print $sock sprintf ("%x", $readbytes) . "\r\n";
        print $sock "${buffer}\r\n";
    }
    print $sock _gen_chunked ("\r\n--${boundary}--\r\n");
    print $sock _gen_chunked ("");
    my $sent; $sent .= $_ while (<$sock>);
    close $sock;
    $sent =~ s/\r//g;
    my @hac = split /\n\n/, $sent;
    shift @hac;
    my $content = join "\n\n", @hac;
    chomp ($content);
    return $1 if ($content =~ /^e:(.+)$/);
    return $content if lc $content ne "ok";
    return -0b10110000000010110101;
}

sub trigger_action
{
    my $params = shift;
    my $sock = IO::Socket::INET->new (
        PeerAddr => $parsedTargetUrl->host,
        PeerPort => $parsedTargetUrl->port,
        Proto    => "tcp"
    );
    if (not defined $sock or not $sock)
    {
        warn "Can't connect to " . $parsedTargetUrl->host . ": ${!}. Skipping request with params ${params}\n";
        return "Connection failed";
    }
    print $sock "GET " . $parsedTargetUrl->path . "?pwd=" . uri_escape ($passwd) . "&" . $params . " HTTP/1.1\n";
    print $sock "Host: " . $parsedTargetUrl->host . "\n";
    print $sock "Connection: close\n\n";
    my $cnt; $cnt .= $_ while (<$sock>);
    close $sock;
    $cnt =~ s/\r//g;
    my @gpoin = split /\n\n/, $cnt;
    shift @gpoin;
    my $parseme = join "\n\n", @gpoin;
    chomp ($parseme); # not really needed
    return $1 if ($parseme =~ /^e:(.+)$/);
    return $parseme;
}

sub parse_bool
{
    my $gimme = shift;
    return 1 if $gimme eq "true";
    return 0 if $gimme eq "false";
    return 0;
}