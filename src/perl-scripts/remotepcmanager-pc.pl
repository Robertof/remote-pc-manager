#!/usr/bin/env perl
# Remote PC Manager - version 2
# PC-hosted daemon
# Responsible of:
# - turning off the PC
# - turning on/off teamviewer
# - taking screenshots

use strict;
use warnings;
use IO::Socket::INET;
use IO::Select;
use threads;
use Digest::SHA1 qw(sha1_hex);
use JSON;
use URI;
use URI::Escape;
use File::Basename;
use File::MimeInfo::Magic;
use File::Spec;
use feature "switch";
use subs qw(ok);
#use Cwd;

my ($bindAddress, $bindPort, $authenticationType, $password, $raspIp, $uri, $pcPass, $shutdownCmd, $shutdownTW, $startTW, $TWStatus, $javaPath, $javaHelperPath, $screenPath, $threaded, $dbg);
my $hardcodedScreenshotName = "rpm-tmp-capture.jpg"; # You should not change this.

# -- CONFIGURATION --
# Enter a valid IP address where to bind and wait for commands.
# You can use 0.0.0.0 to bind to every address. (not recommended)
# NOTE: In most cases you need to bind to your local IP address, for example
# 192.x.y.z or 10.x.y.z. You need to find that address manually (Windows: ipconfig, Linux: ifconfig)
# If you can't find the address / haven't configured a static IP address, you may want to use 0.0.0.0.
$bindAddress = "192.168.1.15";

# Enter a valid port number. It MUST be free and not used by any program.
# Also please note that, if you change the port, you NEED to adjust it in the Raspberry daemon.
# Port forwarding note: the protocol used is TCP.
$bindPort = 18369;

# Select an authentication type between password and ipbased.
# If you use password, you should set the password configuration entry.
# You also need to set it in the Raspberry daemon.
# If you use ipbased, you should set the raspIp variable.
#$authenticationType = "ipbased";
$authenticationType = "password";

# Change this if you are using the 'password' authenticationType.
# ** NOTE ** : The password should be hashed in SHA-1. It wont' work otherwise.
# You should change it in the Raspberry daemon too (by putting the unencrypted one).
# Default password: dummy
$password = "829c3804401b0727f70f73d4415e162400cbe57b";

# Change this if you are using the 'ipbased' authenticationType.
# You should set this to RPi's IP address.
$raspIp = "192.168.1.88";

# Set the target trigger_action.php URL of the web backend
$uri = "http://example.com/trigger_action.php";

# Set the password you configured in the PHP pass for 'pc', in
# plaintext
$pcPass = "1234";

# Set the command which should be used to shutdown the computer.
# NOTE: On windows the default command should be okay, on Linux
# you may want to use 'halt'. However please note that you need root
# permissions to shutdown the computer. You can run the script by
# root (not recommended) or just install something like sudo and add
# a special rule for the halt command.
$shutdownCmd = "shutdown.exe -s -t 0";
#$shutdownCmd = "halt"; # *NIX

# Set the command which should be used to turn off and on TeamViewer.
# NOTE: On windows the default one should work flawlessy with TeamViewer 8
# (if other versions come out simply changing the number should be ok).
# On Linux you may want to find another solution, like using killall
# to shutdown it and starting the process directly to turn it on.
# If you want to disable entirely the TW stuff, just put a -1.
$shutdownTW = "net stop TeamViewer8";
$startTW    = "net start TeamViewer8";
#$shutdownTW = -1;
#$startTW = -1;

# Set the command which should be used to get TeamViewer's status.
# The default implementation works by checking the output of the command.
# If it is empty, TeamViewer is considered to be turned off (otherwise it
# is considered turned on).
# As always, you can disable entirely the TW stuff by putting a -1.
# (this will always return a 'TURNED_OFF' response)
#$TWStatus = "sc query TeamViewer8 | C:\\Windows\\System32\\find.exe \"RUNNING\"";
$TWStatus = "sc query TeamViewer8 | /usr/bin/grep RUNNING"; # for cygwin

# Set the path of Java. If the path is configured in your %PATH% environment
# variable, then you can probably just leave "javaw" here.
# Otherwise specify the full path, escaping the \. Like:
# C:\\Program Files\\Java\\jre7\\bin\\javaw.exe
# ** NOTE **: if you are running Linux, leave java.
# If you are using Windows, use 'javaw'.
# ** NOTE **: if you aren't planning to enable the screenshot
# functionality / you don't have Java, put -1.
$javaPath = "/cygdrive/c/Program Files/Java/jdk1.7.0_09/bin/java.exe";
#$javaPath = -1;

# Set the path of the Java screenshot helper (screenshothelper.jar).
# You may want to leave the default choice (the script path), if you
# didn't move the helper.
# TODO: fix this to properly use File::Spec.
# For now just enter the Windows-path like: C:\\screenshothelper.jar
$javaHelperPath = "C:\\Users\\Roberto\\Documents\\Programmazione\\Perl\\screenshothelper.jar"; #File::Spec->catfile (dirname (__FILE__), "screenshothelper.jar");

# Set the interpreter-dependant screenshot path here.
# (by interpreter-dependant I mean: if you use cygwin, then use the /cygrive/x/ notation, otherwise
#  just C:\\x).
# Remember to put '${hardcodedScreenshotName}' at the end.
# TODO: fix this shit.
$screenPath = "/cygdrive/c/Users/Roberto/Documents/Programmazione/Perl/${hardcodedScreenshotName}";

# True to enable async client handling. Please note that this
# may lead to /potential/ (oh wait, this isn't java) 
# memory leaks for long term runs.
$threaded = 1; # put 0 to disable

# Enable debugging to see what the clients are sending (and when they connect)
$dbg = 1; # put 0 to disable

# -- END OF CONFIGURATION --
# Do not touch any code if you don't know what you are doing.
# Kthxbye.

die ("Configuration error: wrong \$authenticationType\n") if ($authenticationType ne "ipbased" and $authenticationType ne "password");
die ("Configuration error: you are missing the \$raspIp variable\n") if ($authenticationType eq "ipbased" and not defined $raspIp);
die ("Configuration error: you are missing the \$password variable\n") if ($authenticationType eq "password" and not defined $password);
print "> Remote PC Manager PC-hosted daemon is starting...\n";
print "  Configuration details:\n";
print "   Binding on ${bindAddress}:${bindPort}\n";
print "   Authentication type: ${authenticationType}\n";
print "   Java Screenshot helper path: ${javaHelperPath}\n";
#open LOGTMPFILE, ">", "/cygdrive/c/Users/Roberto/rpm.log";
#print LOGTMPFILE "${javaHelperPath} (using ${javaPath} -jar ${javaHelperPath}), pwd " . getcwd();
#close LOGTMPFILE;
my $parsedTargetUrl = URI->new ($uri);
my $hardcodedFieldName = "allyourfilearebelongtous";

$shutdownTW .= " >" . File::Spec->devnull() if $shutdownTW ne -1;
$startTW    .= " >" . File::Spec->devnull() if $startTW    ne -1;

my $bindSocket = IO::Socket::INET->new (
    LocalHost => $bindAddress,
    LocalPort => $bindPort,
    Proto     => "tcp",
    ReuseAddr => 1,
    Listen    => 10
) or die ("Cannot listen to port ${bindPort}: got ${!} (${@})\n");

while (my $client = $bindSocket->accept())
{
    if ($threaded) {
        async (\&handle_client_connection, $client)->detach;
    } else {
        &handle_client_connection ($client);
    }
}

print "> We're no more accepting, goodbye\n";
$bindSocket->close();

sub handle_client_connection
{
    my $client = shift;
    my $addr = $client->peerhost();
    print "> Client connected: ${addr}\n" if $dbg;
    if (!IO::Select->new ($client)->can_read (5)) {
        print " > Connection timed out, closing ${addr}'s connection\n";
        print $client "ERRCODE=4012\n";
        return;
    }
    my $sent = <$client>;
    if (defined $sent) {
        chomp ($sent);
        print " > Client sent: ${sent}\n" if $dbg;
    }
    if (defined $sent and $sent =~ /^AUTHSTRING=([^&]+)&MESSAGE=(.+)$/i) {
        my ($auth, $msg) = ($1, $2);
        if ( ($authenticationType eq "ipbased" and $addr ne $raspIp) or
             ($authenticationType eq "password"and sha1_hex ($auth) ne $password) ) {
            print " > Client ${addr} failed the authentication. GTFOing\n";
            print $client "ERRCODE=3735928559\n"; # db
            return;
        }
        # client is authenticated
        given (lc ($msg))
        {
            when ("pc-alive") {
                return ok $client;
            }
            when ("tw-alive") {
                my $__data;
                if ($TWStatus ne -1) {
                    $__data = `$TWStatus`;
                    chomp ($__data);
                } else {
                    $__data = "";
                }
                if ($__data ne "") {
                    ok $client;
                } else {
                    print $client "ERRCODE=95288014\n"; # 5AD
                }
                return;
            }
            when ("pc-turnoff") {
                print " > Client ${addr} is shutdowning. Bye\n";
                my $ta = trigger_action ("action=pc_immediate_shutdown");
                if ($ta ne -0xB00B5) {
                    warn "An error occurred while running 'pc_immediate_shutdown': ${ta}\n";
                }
                ok $client;
                close $client;
                system ($shutdownCmd);
                return;
            }
            when ("tw-turnoff") {
                return ok $client if ($startTW eq -1);
                print " > Client ${addr} is turning off TeamViewer\n";
                system ($shutdownTW);
                return ok $client;
            }
            when ("tw-turnon") {
                return ok $client if ($startTW eq -1);
                print " > Client ${addr} is turning on TeamViewer\n";
                system ($startTW);
                return ok $client;
            }
            when ("enumerate-monitors") {
                if ($javaPath eq -1) {
                    print $client "ERRCODE=3512383469\n";
                    return;
                }
                my $helperOut = `"${javaPath}" -jar "${javaHelperPath}" enum`;
                chomp ($helperOut);
                #open LOGTMPFILE, ">>", "/cygdrive/c/Users/Roberto/rpm.log";
                #print LOGTMPFILE "\r\nRet of ${javaPath} -jar \"${javaHelperPath}\" enum: ${helperOut}";
                #close LOGTMPFILE;
                if ($helperOut eq "" or $helperOut !~ /:/) {
                    print " > An error occurred, no monitor enumeration :(\n";
                    print $client "ERRCODE=95288014\n";
                    return;
                }
                $helperOut =~ s/\r//g;
                my $monitors = [];
                my @complexlist = split /,/, $helperOut;
                foreach (@complexlist)
                {
                    my ($id, $res) = split /:/, $_;
                    $monitors->[$id] = $res;
                }
                print $client "ERRCODE=61453&DATA=" . &encode_json ($monitors) . "\n";
                return;
            }
            when (/^take\-screenshot(\d+|all)$/) {
                if ($javaPath eq -1) {
                    handle_upload_error();
                    print $client "ERRCODE=3512383469\n"; # d154
                    return;
                }
                my $monitor = uc ($1);
                print " > Client ${addr} is going to take a screenshot on monitor ${monitor}\n";
                my $helperOut = `"${javaPath}" -jar "${javaHelperPath}" ${monitor}`;
                chomp ($helperOut);
                #my $pathname = File::Spec->catfile (dirname ($javaHelperPath), $hardcodedScreenshotName);
                if ($helperOut ne "" or !-e $screenPath) {
                    print " > An error occurred! ${helperOut}\n";
                    handle_upload_error();
                    print $client "ERRCODE=95288014\n";
                    return;
                }
                ok $client;
                close $client;
                print " > OK, uploading the screenshot (${screenPath})\n";
                my $upoutput = send_dat_file ("${uri}?pwd=${pcPass}&action=upload&context=screenshot", $screenPath, $hardcodedFieldName);
                if ($upoutput ne -0xB00B5) {
                    print " > An error occurred while uploading the screenshot: ${upoutput}\n";
                    handle_upload_error();
                    #print $client "ERRCODE=95288014\n";
                    return;
                }
                print " > Done.\n";
            }
            default {
                print " > Client ${addr} sent an invalid message (${msg}). GTFOing\n";
                print $client "ERRCODE=4207299351\n"; # dib
            }
        }
    } else {
        print " > Client ${addr} sent an invalid string. GTFOing\n";
        print $client "ERRCODE=2975531189\n"; # bb
    }
    return;
}

sub ok {
    print {$_[0]} "ERRCODE=61453\n";
}

sub _gen_chunked
{
    my $str = shift;
    my $length = sprintf ("%x", length ($str));
    return "${length}\r\n${str}\r\n";
}

sub handle_upload_error
{
    trigger_action ("action=upload&context=screenshot&emergency-error=true");
}

# Usage: send_dat_file ("http://...", "path/to/file.png", "filefield")
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
    print $sock "GET " . $parsedTargetUrl->path . "?pwd=" . uri_escape ($pcPass) . "&" . $params . " HTTP/1.1\n";
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
    return $parseme if lc $parseme ne "ok";
    return -0b10110000000010110101;
}