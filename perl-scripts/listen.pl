use strict;
use warnings;
use IO::Socket::INET;

my $server = IO::Socket::INET->new (
    LocalPort => "1337",
    LocalHost => "INSERT_YOUR_INTERNAL_IP_ADDR",
    Proto     => "tcp",
    ReuseAddr => 1,
    Listen    => 1
) || die "Cannot bind: $!";

while (my $client = $server->accept())
{
    my $addr = $client->peerhost();
    if ($addr ne "192.168.1.1") {
        $client->close();
        next;
    }
    my $data = <$client>;
    print $client "HTTP/1.1 200 OK\n";
    print $client "Content-Type: text/html; charset=utf-8\n";
    print $client "Content-Length: 0\n\n";
    $client->close();
    if ($data =~ /GET\s\/do_shutdown\s/i)
    {
        system ("INSERT_CANONICAL_PATH_OF_PSSHUTDOWN/psshutdown.exe -t 0 -d");
    }
}