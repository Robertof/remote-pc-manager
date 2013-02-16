<?php
$key = "** INSERT YOUR KEY **";
if (!isset ($_GET['key']) || (isset ($_GET['key']) && sha1 ($_GET['key']) != $key))
	die ("Wrong key");
print file_get_contents ('status.txt');
$fp = fopen ('status.txt', 'w');
fwrite ($fp, "-1");
fclose ($fp);
?>
