<?php
$key = "** INSERT YOUR KEY **";
if (!isset ($_GET['key']) || (isset ($_GET['key']) && sha1($_GET['key']) != $key))
	die ("Wrong key");
if (!isset ($_GET['val']) || !is_numeric ($_GET['val']))
	die ("Wrong value");
$v = intval($_GET['val']);
// -1 idle, 0 shutdown, 1 turn on
if ($v != 0 && $v != 1)
	die ("Wrong value, 0||1");
$f = fopen ("status.txt", "w");
fwrite ($f, $v);
fclose ($f);
print "OK";
?>
