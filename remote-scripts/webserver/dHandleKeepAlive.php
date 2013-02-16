<?php
$pckey = "** INSERT YOUR KEY **";
if (isset ($_GET['kKey']) && sha1($_GET['kKey']) == $pckey)
{
	$fp = fopen ("last_keepalive.txt", "w");
	fwrite ($fp, time());
	fclose ($fp);
}
?>
