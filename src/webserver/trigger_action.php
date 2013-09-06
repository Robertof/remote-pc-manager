<?php
// Remote PC Manager -- version 2
// Author: as always, Robertof
// General action handler -- specifies wrappers for each
// known action. Also provides getters for statuses

require_once "config.php";
if (!isset ($_GET["action"]) || !isset ($_GET["pwd"]) ||
    ( ( isset ($_GET['action']) && trim ($_GET['action']) == '' ) || ( isset ($_GET['pwd']) && trim ($_GET['pwd']) == '' )))
    RemotePCControl::fatalError ("Missing parameters.");
$actionName = strtolower (trim ($_GET['action']));
if (!RemotePCControl::isActionEnabled ($actionName))
    RemotePCControl::fatalError ("Invalid action name / disabled action");
define ('TURN_ON',  1);
define ('TURN_OFF', 0);
define ('NOTHING', -1);
define ('UPLOAD_KEY', 'allyourfilearebelongtous');

$actions = array (
    'screenshot' => new ScreenshotAction(),
    'webcam'     => new WebcamImgAction()
);

switch ($actionName)
{
    case "turn_off_pc":
    case "turn_on_pc":
    case "enable_teamviewer":
    case "disable_teamviewer":
        // perform a status update
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("app"))
            RemotePCControl::fatalError ("Wrong password!");
        $data = StatusHandler::readStatus();
        $sName = getServiceName ($actionName, ( strpos ($actionName, "teamviewer") !== false ? 1 : 2));
        $_enable = preg_match ("/enable|on/", $actionName);
        if (isset ($data["{$sName}-pendingStatus"]) && $data["{$sName}-pendingStatus"] != NOTHING)
            RemotePCControl::fatalError ("A status update is already pending: " . $data["{$sName}-pendingStatus"], true);
        if (is_alive ($sName, $data) == $_enable)
            RemotePCControl::fatalError ("Your service is already " . ( $_enable ? "turned on" : "turned off" ), true);
        $data["{$sName}-pendingStatus"] = $_enable ? TURN_ON : TURN_OFF;
        StatusHandler::updateAndWriteStatus ($data);
        echo "OK";
    break;
    case "read_statuses":
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi"))
            RemotePCControl::fatalError ("Wrong password!");
        $data = StatusHandler::readStatus();
        $s1 = getStatusChangeNeeded ($data, "pc");
        $s2 = getStatusChangeNeeded ($data, "teamviewer");
        $bShouldUpdate = false;
        $tw_status = is_alive ("teamviewer", $data);
        $pc_status = is_alive ("pc",         $data);
        if (!$pc_status && $tw_status)
        {
            $data["teamviewer-lastKA"] = time() - 91;
            StatusHandler::updateAndWriteStatus ($dbd);
        }
        if ($s1 != NOTHING || $s2 != NOTHING)
            StatusHandler::updateAndWriteStatus ($data);
        echo json_encode (array ("pc" => $s1, "teamviewer" => $s2, "pc_alive" => is_alive ("pc", $data), "teamviewer_alive" => is_alive ("teamviewer", $data)));
    break;
    case "ping":
        if (!isset ($_GET['target']) || !in_array ($_GET['target'], array ('pc', 'teamviewer', 'pc,teamviewer')))
            RemotePCControl::fatalError ("Missing parameters.");
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi"))
            RemotePCControl::fatalError ("Wrong password!");
        $db = StatusHandler::readStatus();
        $gdb = explode (',', $_GET['target']);
        foreach ($gdb as $serv)
            $db["{$serv}-lastKA"] = time();
        StatusHandler::updateAndWriteStatus ($db);
        echo "OK";
    break;
    case "alive":
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("app"))// &&
            //sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi"))
            RemotePCControl::fatalError ("Wrong password!");
        $dbd = StatusHandler::readStatus();
        $tw_status = is_alive ("teamviewer", $dbd);
        $pc_status = is_alive ("pc",         $dbd);
        if (!$pc_status && $tw_status)
        {
            $dbd["teamviewer-lastKA"] = time() - 91;
            StatusHandler::updateAndWriteStatus ($dbd);
        }
        echo json_encode (array ("pc" => $pc_status, "teamviewer" => $tw_status));
    break;
    case "pc_immediate_shutdown":
    case "teamviewer_immediate_shutdown":
        $dk = getServiceName ($actionName, 0);
        #print $dk;
        if (($dk == "pc" && sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("pc")) ||
            ($dk == "teamviewer" && sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi")))
            RemotePCControl::fatalError ("Wrong password!");
        $db = StatusHandler::readStatus();
        if (!is_alive ($dk, $db)) die ("OK");
        $db["{$dk}-lastKA"] = time() - 91;
        StatusHandler::updateAndWriteStatus ($db);
        echo "OK";
    break;
    case "do_action":
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("app"))
            RemotePCControl::fatalError ("Wrong password!");
        if (!isset ($_GET['param']) || !array_key_exists ($_GET['param'], $actions))
            RemotePCControl::fatalError ("Missing parameters.");
        $actionObj = $actions[$_GET['param']];
        if (!$actionObj->sent_data_valid())
            RemotePCControl::fatalError ("Missing parameters.");
        $db = StatusHandler::readStatus();
        if (!$actionObj->canWrite ($db))
            RemotePCControl::fatalError ("Can't write: " . $actionObj->getWhyCantWrite(), true);
        $res = $actionObj->write ($db);
        if (!is_bool ($res))
            RemotePCControl::fatalError ($res, true);
        if ($res)
            StatusHandler::updateAndWriteStatus ($db);
        echo "OK";
    break;
    case "read_scheduled_actions":
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi"))
            RemotePCControl::fatalError ("Wrong password!");
        $db = StatusHandler::readStatus();
        $to_serialize = array();
        $written = false;
        foreach ($actions as $name => $obj)
        {
            $tmpres = $obj->read ($db);
            if (!$written && $tmpres['dbedited'])
                $written = true;
            $to_serialize[$name] = $tmpres['response'];
        }
        if ($written)
            StatusHandler::updateAndWriteStatus ($db);
        echo json_encode ($to_serialize);
    break;
    case "upload":
        if (!isset ($_GET['context']) || !in_array ($_GET['context'], array ("screenshot", "webcam")))
            RemotePCControl::fatalError ("Missing parameters.");
        $context = $_GET['context'];
        if ((isset ($_GET['emergency-error'])  && !in_array (sha1 ($_GET['pwd']), array (RemotePCControl::getPassword ("raspi"), RemotePCControl::getPassword ("pc")))) ||
            (!isset ($_GET['emergency-error']) && $context == "webcam" && sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi")) ||
            (!isset ($_GET['emergency-error']) && $context == "screenshot" && sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("pc")))
            RemotePCControl::fatalError ("Wrong password!");
        $path = RemotePCControl::getProperDataDirPath() . $context . "-sent-img.jpg";
        if (isset ($_GET['emergency-error']) && empty ($_FILES))
        {
            if (file_exists ($path))
                @unlink ($path);
            $db = StatusHandler::readStatus();
            $db["{$context}-error"] = time();
            StatusHandler::updateAndWriteStatus ($db);
            echo "OK";
            exit;
        }
        /*if (file_exists ($path) && (time() - filemtime ($path)) < 90)
            RemotePCControl::fatalError ("An image was already uploaded, please let the client download the previous one", true);
        else if (file_exists ($path) && (time() - filemtime ($path)) > 90)
            unlink ($path);*/
        if (file_exists ($path))
            RemotePCControl::fatalError ("An image was already uploaded, please let the client download the previous one", true);
        if (empty ($_FILES) || !isset ($_FILES[UPLOAD_KEY]) || empty ($_FILES[UPLOAD_KEY]) || $_FILES[UPLOAD_KEY]['error'] != 0)
            RemotePCControl::fatalError ("You didn't send a file. Are you okay son?", true);
        if ($_FILES[UPLOAD_KEY]['type'] != 'image/jpeg' || getext ($_FILES[UPLOAD_KEY]['name']) != "jpg")
            RemotePCControl::fatalError ("You didn't send an image. I'm disappointed.", true);
        $imgPath = $_FILES[UPLOAD_KEY]['tmp_name'];
        $_imgdata = @getimagesize ($imgPath);
        if (!is_array ($_imgdata) || $_imgdata['mime'] != 'image/jpeg')
            RemotePCControl::fatalError ("You didn't send an image. I'm really disappointed.", true);
        if (!@move_uploaded_file ($imgPath, $path))
            RemotePCControl::fatalError ("Can't move your image. I'm sorry.", true);
        echo "OK";
    break;
    case "uploaded":
        if (!isset ($_GET['context']) || !in_array ($_GET['context'], array ("screenshot", "webcam")))
            RemotePCControl::fatalError ("Missing parameters.");
        $context = $_GET['context'];
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("app"))
            RemotePCControl::fatalError ("Wrong password!");
        $path = RemotePCControl::getProperDataDirPath() . $context . "-sent-img.jpg";
        $db = StatusHandler::readStatus();
        if (isset ($db["{$context}-error"]) && $db["{$context}-error"] > 0)
        {
            // if an uploaded file is there and it's more recent than the date when
            // the error was reported, clean the error status and let it go
            if (file_exists ($path) && filemtime ($path) > $db["{$context}-error"])
                $db["{$context}-error"] = 0;
            else
            {
                $db["{$context}-error"] = 0;
                StatusHandler::updateAndWriteStatus ($db);
                RemotePCControl::fatalError ("An error occurred while taking or uploading the screenshot", true);
            }
            StatusHandler::updateAndWriteStatus ($db);
        }
        if (file_exists ($path) && (time() - filemtime ($path)) > 90)
            unlink ($path);
        if (!file_exists ($path)) {
            echo "NOPE";
            exit;
        }
        echo "OK";
        //header ("Content-type: image/jpeg");
        //echo file_get_contents ($path);
        //unlink ($path);
    break;
    case "get_uploaded_image":
        if (!isset ($_GET['context']) || !in_array ($_GET['context'], array ("screenshot", "webcam")))
            RemotePCControl::fatalError ("Missing parameters.");
        $context = $_GET['context'];
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("app"))
            RemotePCControl::fatalError ("Wrong password!");
        $path = RemotePCControl::getProperDataDirPath() . $context . "-sent-img.jpg";
        if (!file_exists ($path))
            die ("NOPE");
        header ("Content-type: image/jpeg");
        echo file_get_contents ($path);
        unlink ($path);
    break;
    case "upload_monitor_info":
        if (!isset ($_GET['data']) || empty ($_GET['data']))
            RemotePCControl::fatalError ("Missing parameters.");
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("raspi"))
            RemotePCControl::fatalError ("Wrong password!");
        $dd = json_decode ($_GET['data'], true);
        if (!$dd || !is_array ($dd))
            RemotePCControl::fatalError ("Missing parameters.");
        $db = StatusHandler::readStatus();
        $db['monitor-data'] = $dd;
        StatusHandler::updateAndWriteStatus ($db);
        echo "OK";
    break;
    case "enumerate_monitors":
        if (sha1 ($_GET['pwd']) !== RemotePCControl::getPassword ("app"))
            RemotePCControl::fatalError ("Wrong password!");
        $db = StatusHandler::readStatus();
        if (!isset ($db['monitor-data']))
            die ("[]");
        echo json_encode ($db['monitor-data']);
    break;
    default:
        echo "Who the f*** configured me? Y DID U PUT A NON-EXISTING ACTION?";
}

// $data is the database
function getStatusChangeNeeded (&$data, $k)
{
    $k .= "-pendingStatus";
    if (isset ($data[$k]) && $data[$k] != NOTHING)
    {
        $queued = $data[$k];
        $data[$k] = NOTHING;
        return $queued;
    }
    else
        return NOTHING;
}

function getext ($name)
{
    $gpoint = explode (".", $name);
    if (count ($gpoint) < 2) return -1;
    return array_pop ($gpoint);
}

function is_alive ($sName, $db = null)
{
    if (!$db)
        $db = StatusHandler::readStatus();
    $k = "{$sName}-lastKA";
    if (!isset ($db[$k]) || !is_numeric ($db[$k]))
        return false;
    return ( time() - $db[$k] ) < 90;
}

function getServiceName ($aName, $underscorePos)
{
    // * Kept because it's just too epic (before I realized I could use explode) * return substr ($aName, ($_lpos = ( ( $__f = function ($aName, $underscorePos) { $q = -1; for ($i = 0; $i < $underscorePos; $i++) { $q = strpos ($aName, "_", $q + 1); }; return $q; } ) && $underscorePos != 0 && $underscorePos != substr_count ($aName, "_") ? $__f ($aName, $underscorePos) : ( $underscorePos == 0 ? -1 : $__f ($aName, substr_count ($aName, "_"))) ) ) + 1, ( substr_count ($aName, "_") != $underscorePos ? strpos ($aName, "_", $_lpos + 1) - 1 - $_lpos : ($_sheit = -1))) . (isset ($_sheit) ? $aName[strlen ($aName) - 1] : "");
    $g = explode ("_", $aName);
    if (count ($g) - 1 < $underscorePos) return;
    return $g[$underscorePos];
}
?>
