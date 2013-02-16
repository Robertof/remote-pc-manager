# Robertof's WOL daemon
# Waits on a port for shutdown instructions
# and sends keepalives to a remote server
# > keepalive daemon
use strict;
use IO::Socket::INET;
use Time::Piece;

my $key = '** INSERT YOUR DHANDLEKEEPALIVE.PHP KEY HERE **';
my $lfile = "keepalives_sent.log";
my $internal_count = 0;

&rotate_logfile;

while (1)
{
    my $sock = IO::Socket::INET->new (
        PeerHost => "INSERT_YOUR_HOST",
        PeerPort => 80,
        Proto    => "tcp"
    ) or next;
    # send an HTTP request
    print $sock "GET /INSERT_YOUR_PATH/dHandleKeepAlive.php?kKey=${key} HTTP/1.1\n";
    print $sock "Host: INSERT_YOUR_HOST\n\n";
    close $sock;
    &update_count;
    sleep 75;
}

sub update_count
{
    open MAIFAIL, ">", $lfile;
    my $_date = Time::Piece->new->strftime ("%d/%m/%Y");
    print MAIFAIL "${_date}\r\n" . ++$internal_count;
    close MAIFAIL;
}

sub rotate_logfile
{
    if (-e $lfile)
    {
        my $newname = "";
        if (!-e "${lfile}.1") {
            $newname = "${lfile}.1";
        } elsif (!-e "${lfile}.2") {
            $newname = "${lfile}.2";
        } elsif (!-e "${lfile}.3") {
            $newname = "${lfile}.3";
        } else {
            unlink "${lfile}.1";
            unlink "${lfile}.2";
            unlink "${lfile}.3";
            $newname = "${lfile}.1";
        }
        rename ($lfile, $newname);
    }
}
