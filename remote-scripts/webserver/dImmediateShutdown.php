<?php
$key = "** INSERT YOUR KEY **";
if (!isset ($_GET['key']) || (isset ($_GET['key']) && sha1($_GET['key']) != $key))
	die ("Wrong key");
$cb = intval(file_get_contents("last_keepalive.txt")) - 91;
$fb = fopen ("last_keepalive.txt", "w");
fwrite ($fb, $cb);
fclose($fb);
?>

