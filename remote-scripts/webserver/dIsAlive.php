<?php
$k = file_get_contents("last_keepalive.txt");
if ( ( time() - intval ($k) ) < 90 )
	print "ON";
else
	print "OFF";
?>
