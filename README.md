Robertof's Remote PC Control project
================================
What is this thing?
-------------------------

This is a project I started working on some weeks ago.
What I needed was a thing that would let me turn on and off my PC remotely.
Not finding anything online, I decided to build it by myself. Here comes Remote PC Control. (sorry for the bad name)

How it works?
-------------------------

The PC to turn on is waked by a Wake On LAN packet and it is turned off by a daemon.
All the stuff is splitted in 5 different parts:
* An Android app which is used to control everything.
* A PHP helper on a remote webserver, which handles stuff sent by the app and the Perl script.
* A Perl backend/daemon which turns off the PC when requested and sends keepalives to the PHP helper.
* An SH backend, placed on an always-on machine (in my case a router), which sends the WOL packet / tells the Perl daemon to shutdown the PC.
* TeamViewer, always running on the target PC, which provides effective remote control.

When you click "turn on" in the Android app, this is what happens:
* an HTTP request is sent to the PHP helper on the remote server
* the PHP helper receives the request and waits for the SH backend to retrieve the status (turn on the PC)
* the SH daemon receives the status and performs the right action (in this case: sends the WOL packet to the PC)

Meanwhile the Perl backend on the PC sends keepalives to the PHP helper, so that is considered ON by the app.
When you click "turn off" the things are similar, but instead of sending the WOL packet it sends the shutdown request to the Perl backend.
**NOTE**: since my motherboard does not support keepalive when the PC is in a full-shutdown state, the script actually suspends it.

Details about the Android app
-----------------------------

I built the app against Android 4.0 SDK. This is because I own a Galaxy Nexus, and I was too lazy to adapt the app to an older SDK version.
However if you want to, feel free to do it. That's why open source rocks.
By the way, the app asks for a password on start. You can define that password in the sourcecode, hashed in SHA1.
Also in the source code you have to define the TeamViewer password if you wanna use TeamViewer. Otherwise, just remove the 2 TW-related buttons.
I translated it in Italian and English.

Details about the PHP helper
----------------------------

The PHP helper is a simple set of PHP files which can "connect" the various parts of this project.
Here is detailed stuff about each PHP file:
* dHandleKeepAlive.php: handles keepalives sent by the Perl backend on the PC. It simply saves in a file called last_keepalive.txt the latest timestamp of the PC's keepalive. It REQUIRES a key set in the source code (hashed in SHA-1 as every other key).
* dIsAlive.php: returns "ON" if the PC is on (aka if the last keepalive timestamp - current timestamp < 90) or "OFF" it is not. Does NOT require a key (for lazyness reasons).
* dImmediateShutdown.php: called right after the shutdown request is sent to the PC to avoid unuseful waitings (aka: to avoid waiting for the keepalive to expire). Again, it REQUIRES a key.
* dTriggerStatusChange.php: called when the user turns on/off the PC from the Android app. Saves in a textfile "1" if the SH backend should turn on the PC, "0" if it should turn off the PC and "-1" if nothing more is needed. This REQUIRES a key.
* dReadStatus.php: reads status from the textfile saved by dTriggerStatusChange. Sets it to -1 after it has been printed. REQUIRES a key.

Details about the Perl stuff
----------------------------

There are 2 Perl scripts which are always running on the target PC.
Here are the details of each script:
* listen.pl: listens on a port on your internal IP address (you have to specify it in the sourcecode) and waits for the backend (and ONLY the backend - it checks the IP) to send the shutdown request. Actually it creates a fake HTTP server for compatibility with cURL (I had only cURL on my router): when the request is accepted it outputs an empty HTTP reply, otherwise it kills the connection.
* daemon.pl: sends keepalives to the PHP helper. It provides a basic (and BROKEN) log rotation. Why broken? Because when the log count is > 3, it DELETES them. Sorry but I was too lazy to implement a proper logrotation.

Details about the SH backend
----------------------------

The SH script was made to be very light since in my case it was running on an home router with only 120MB of RAM.
It constantly reads status from the PHP helper and performs the proper action.
In the sourcecode you have to specify the keys used in dImmediateShutdown and dReadStatus, the MAC address of the target PC and the interface which should send the packet.
Status codes:
* 1: the user requested to turn on the PC. It runs /usr/bin/ether-wake (was installed by default on my router) on the target PC MAC address.
* 0: the user requested to turn off the PC. It connects to the Perl daemon in the target PC and sends the shutdown request. Also calls dImmediateShutdown to set the turned off state of the PC.
* -1: nothing to do.

Additional notes
----------------

I built everything on Windows. The Perl stuff runs from Cygwin's Perl executable, which I found is the less memory expensive Perl implementation (it consumes about 3MB of RAM with one script).

How to setup everything
-----------------------

Setting up this is a bit complex. Also, you need a bit of programming skills to edit that. Ready? First, you need the following stuff:
* A working Perl installation on the target PC, with support of sockets (binding & connecting)
* A working WOL implementation on the target PC (motherboard dependant)
* An always-on machine which can dispatch requests to the PC (and supports SH, cURL and has ether-wake)
* A webserver which can host PHP files
* An Android phone with at least ICS (4.0)

Then replace every "INSERT ***" variable in the following files (thanks grep):
* android-app/res/values/strings.xml
* android-app/src/it/robertof/rpcc/*.java
* perl-scripts/*.pl
* remote-scripts/router/daemon.sh
* remote-scripts/webserver/dHandleKeepAlive.php
* remote-scripts/webserver/dImmediateShutdown.php
* remote-scripts/webserver/dReadStatus.php
* remote-scripts/webserver/dTriggerStatusChange.php

After you have edited everything, you're ready to start.
Start by putting the PHP helper in a private space. If possible, disable global access on every *.txt file.
Test everything by calling http://yourwebserver.com/path/dIsAlive.php. It should print "OFF".
Now prepare your Perl environment on the target PC and
* if you are running Linux, find some way on your distribution to start 2 perl scripts when the PC starts.
* if you are running Windows, use Window's built in task scheduler. Set it to start the Perl processes on the PC startup.

Reboot to apply the changes.
Now, place the daemon in your always-on machine and start it with something like "nohup sh daemon.sh &".
Finally, build the Android app, place it on your phone and try to shutdown the PC. If it works, you're done!
Otherwise, something may be wrong. Check if you did every step correctly!

TODO
----

* More stuff in client's Perl backend and the Android app (planned: screenshots)

License
-------

Personally I wrote this only to have fun and challenging myself. So you can do whatever you want with this, but please, give attribution. And if you want, warn me when you use that in a project.
Thanks!

EOF
---

You have reached the end of this big manual. I hope you enjoyed reading (if you read at all). Send me pull requests/messages if you want to fix/know something.
Enjoy