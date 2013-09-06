Robertof's Remote PC Manager project
================================
What is this thing?
-------------------------

A bit of time ago, just for fun, I decided to create a project which allowed me to turn on and off my PC remotely.

However, I decided that the project could have been expanded even more. So, here it is: Remote PC Manager v2 (before Remote PC Control), with support for TeamViewer remote enabling/disabling and the possibility to take remote screenshots/webcam images.

How it works?
-------------------------

Before the v2 update, the project was using the Wake On LAN method to turn on my PC. However, my motherboard is configured to only allow Wake on LAN packets while the PC is suspended. So, I came up with an 'hardcore' solution: Raspberry Pi + PiFace + two cables coming out from my motherboard PWR input and going into a relay.

By the way, everything is managed by 4 different components:
* An Android app which is used to manage everything. (taking screenshots, turning on/off the PC, disabiling TW, etc.)
* A PHP helper on a remote webserver, which basically acts as a 'message delivery service'.
* Two Perl scripts, one on the target computer (which handles actions like turning off the PC/TeamViewer and taking screenshots) and one on the Raspberry Pi (the main one, turns on the PC, takes webcam images and manages everything).

When you click "turn on" in the Android app, this is what happens:
* an HTTP request is sent to the PHP helper on the remote server
* the Perl daemon on the Raspberry receives the request and enables for 0.2 seconds the relay connected to the PWR input.

When the PC is fully turned on, the Raspberry daemon sends a keepalive to the PHP helper, and the app sets the PC status to 'ON'.

Details about the Android app
-----------------------------

I built the app against Android 4.0 SDK. This is because I own a Galaxy Nexus, and I was too lazy to adapt the app to an older SDK version.
However if you want to, feel free to do it.

The app asks for a password on start (which is defined with a SHA-1 hash in ControlActivity) and communicates with the PHP helper.

Also when the user requests a screenshot or webcam request, the app automatically waits for the screenshot to be uploaded and downloads it with the built-in DownloadManager.

I translated it in Italian and English.

Details about the PHP helper
----------------------------

The PHP helper is a pair of PHP files which act as a 'medium' between the various components.
There are only two PHP files, and those are:
* `config.php`: where you can define the configuration options, like the hashes for each component.
* `trigger_action.php`: the file which handles every action. You should not edit this unless needed.

Details about the Perl stuff
----------------------------

There are two Perl scripts, one running on the Raspberry Pi and one on the PC.
Here are the details of each script:
* `remotepcmanager-raspi.pl`: as the name suggests, this is the daemon running on the Raspberry Pi. While starting, it asks the PC daemon for monitor data (if the PC daemon is not available or turned on, it disables the possibility of taking screenshots. This may change in a near future). While running, it iterates every 10 seconds and asks the PHP helper if there are any actions he should do (for example, turning on something or taking an image). Then he dispatches the request to the appropriate daemon (or just executes it by himself). Please note that to allow taking of webcam images, the script uses an external executable called 'capture.cpp'. Install the basic OpenCV libraries and then compile it with the command specified on the file header. Then configure the Perl script accordingly. Last but not less important, to turn on the PC I used my [piface-perl](http://github.com/Robertof/piface-perl) library. For now, you NEED it otherwise the script won't run. This may change.
* `remotepcmanager-pc.pl`: this script is running on the PC, and creates a TCP server on the port specified in the config. It handles screenshots, TeamViewer stuff and it turns off the PC when needed. Note that this script WON'T work without the Raspberry daemon. Also please note that to take screenshots this script uses an external Java-based executable, called `screenshothelper.jar` (source code included in the jar). Please configure the Java and the jar path as needed in the script file (you can disable the screenshot stuff if you don't need it). Last, to turn on/off/query TeamViewer, I used some built-in Windows executables. If you are using Linux, then you may need a bit of work to adjust this.

Additional notes
----------------

I built everything on Windows. The Perl stuff runs from Cygwin's Perl executable, which I found is the less memory expensive Perl implementation (it consumes about 3MB of RAM with one script).

How to setup everything
-----------------------

Setting up this may be a little bit complex but not as hard as the old implementation. Those are the components you need to get started:
* A working Perl installation on the target PC
* A Raspberry Pi with a [PiFace](http://pi.cs.man.ac.uk/) on it and Perl.
* Two cables connected to your motherboard's PWR inputs.
* A webserver which can host PHP files.
* An Android phone with at least ICS (4.0)

First, configure the following files as needed (they are well documented so you shouldn't have any difficulties):
* android-app/src/it/robertof/rpm/ControlActivity.java
* perl-scripts/remotepcmanager-pc.pl
* perl-scripts/remotepcmanager-raspi.pl
* webserver/config.php

After you have edited everything, you're ready to start.
Start by putting the PHP helper in a private space. If you are using Apache and you have enabled the auto htaccess creation, you're fine. Otherwise limit access to the 'private' directory.
Test if there aren't any errors. Request http://example.com/trigger_action.php. You should see a string starting with 'e:'.

Put the 'remotepcmanager-raspi.pl' file in your Raspberry, along with capture.cpp. Compile capture.cpp with the command specified in the header of the file and put it in the same directory of 'remotepcmanager-raspi.pl'.
If you need to start automatically the Perl daemon on your Raspberry, then edit the included 'rpm.service' file (by adjusting the path of your RPi daemon), move it in `/usr/lib/systemd/system/rpm.service` and run `systemctl enable rpm`.

Now prepare your Perl environment on the target PC and
* if you are running Linux, find some way on your distribution to start a perl script when the PC starts.
* if you are running Windows, use Window's built in task scheduler. Set it to start the Perl process on the PC startup.

Reboot and check if you have perl in your process list.

Finally, build the Android app, place it on your phone and try to shutdown the PC. If it works, you're done!
Otherwise, something may be wrong. Check if everything is configured and started properly. Contact me if you need further help.

License
-------

Every part of this project is licensed under the BSD-2 license. You can find a copy of it in the `LICENSE` file.

EOF
---

You have reached the end of this big manual. I hope you enjoyed reading (if you read at all). Send me pull requests/messages if you want to fix/know something.
Have fun.