<?php
// Remote PC Manager version 2
// Global configuration file
// Author: Robertof

class RemotePCControl
{
    // !! CONFIG BEGINS NOW !!
    // Various passwords. Hash them in SHA1 (php.net/sha1 || echo -n 'pwd' | sha1sum)
    private static $cfg_passwords = array (
        "pc"    => "b791d80a917240fc266795048a59383efe1747f3", // [pcpass]    password of the Perl daemon hosted on the PC
        "raspi" => "bf12da00b7937f43bdd727a652bb502fb0e627d4", // [raspipass] password of the Raspberry Pi daemon
        "app"   => "9c1f0f771f8a32f2476c6ed3171d75cbd84d286f"  // [apppass]   password of the Android app
    );

    // Available actions. Put a '//' before an action to disable it.
    private static $cfg_enabled_actions = array (
        "turn_off_pc",
        "turn_on_pc", // you probably want both of them enabled
        "enable_teamviewer",
        "disable_teamviewer",
        "do_action", // for screenshot and webcam
        "upload",    // ^
        "uploaded",  // ^
        "get_uploaded_image", // ^
        // -- don't disable those actions or the world will explode --
        "read_statuses",
        "alive",
        "ping",
        "pc_immediate_shutdown",
        "teamviewer_immediate_shutdown",
        "read_scheduled_actions",
        "upload_monitor_info",
        "enumerate_monitors"
    );

    // True if you are in a production environment.
    // * no errors / generic error messages
    private static $cfg_production_env = false;

    // Define the directory where private data is stored.
    // NOTE: a proper htaccess is created for that directory automatically.
    // You should configure properly your webserver if it isn't an Apache based one.
    // The directory is automatically created if it doesn't exist.
    private static $cfg_data_dir = "private/";

    // Define here the default permissions of the data directory. It will be used for
    // the files too.
    // NOTE: Use Octal notation (0xxx)
    // Recommended: 0700 - read/write/execute for the owner, none for the group, none for the others
    private static $cfg_data_dir_perm = 0700;

    // True if you want to automatically create an .htaccess in the private data
    // directory. If you do not use Apache, be sure to find a way for your webserver
    // to disable access to the $cfg_data_dir path.
    private static $cfg_create_htaccess = true;

    // !! CONFIG ENDS NOW !!

    // Helper functions
    public static function printError ($str, $skipProductionCheck = false)
    {
        if (self::$cfg_production_env && !$skipProductionCheck)
            $str = "An error has occurred.";
        echo "e:", $str;
    }

    public static function fatalError ($str, $skipProductionCheck = false)
    {
        self::printError ($str, $skipProductionCheck);
        exit;
    }

    public static function initPrivateDir()
    {
        if (!is_dir (self::$cfg_data_dir))
        {
            mkdir (self::getProperDataDirPath (false), self::$cfg_data_dir_perm);
            self::createPrivateDirHtaccess();
        }
        else if (!file_exists (self::getProperDataDirPath() . ".htaccess"))
            self::createPrivateDirHtaccess();
    }

    public static function storeDataToJson ($fname, $data)
    {
        $__name = self::getProperDataDirPath() . $fname;
        $flagExists = file_exists ($__name);
        $pointer = fopen ($__name, "w");
        if (!$pointer)
            self::fatalError ("Cannot create {$fname}!");
        fwrite ($pointer, json_encode ($data));
        fclose ($pointer);
        if (!$flagExists)
            chmod ($__name, self::$cfg_data_dir_perm) || self::fatalError ("Cannot chmod {$fname} :(");
    }

    public static function getDataFromJson ($fname, $asObject = false)
    {
        $path = self::getProperDataDirPath() . $fname;
        if (!file_exists ($path))
            return null;
        return json_decode (file_get_contents ($path), !$asObject);
    }

    public static function isActionEnabled ($needle)
    {
        return in_array ($needle, self::$cfg_enabled_actions);
    }

    public static function getPassword ($service)
    {
        if (!array_key_exists ($service, self::$cfg_passwords)) return;
        return self::$cfg_passwords [$service];
    }

    // internal functions
    private static function createPrivateDirHtaccess()
    {
        if (!self::$cfg_create_htaccess) return;
        // who called us should know what he is doing, so we are trusting him
        // and we don't check if another htaccess exists
        $__path = self::getProperDataDirPath() . ".htaccess";
        $pointer = fopen ($__path, "w");
        if (!$pointer)
            self::fatalError ("Cannot create .htaccess into your private dir. :(");
        fwrite ($pointer, <<<NICEHTACCESS
Order deny,allow
Deny from All
NICEHTACCESS
        );
        fclose ($pointer);
        if (!chmod ($__path, self::$cfg_data_dir_perm))
            self::fatalError ("Cannot set the correct permissions on your file. :(");
    }

    public static function getProperDataDirPath ($wantTrailing = true)
    {
        return ( substr (self::$cfg_data_dir, -1) == "/" ? ( $wantTrailing ? self::$cfg_data_dir : substr (self::$cfg_data_dir, 0, -1) ) : ( $wantTrailing ? self::$cfg_data_dir . "/" : self::$cfg_data_dir ));
    }

    // ** getters for class variables **
    public static function getPasswords()
    {
        return self::$cfg_passwords;
    }

    public static function getEnabledActions()
    {
        return self::$cfg_enabled_actions;
    }

    public static function getIsInProductionEnv()
    {
        return self::$cfg_production_env;
    }
}

class StatusHandler
{
    private static $statusFile = "status.json";
    private static $status;
    public static function readStatus()
    {
        self::$status = RemotePCControl::getDataFromJson (self::$statusFile);
        if (!self::$status)
            self::$status = array();
        return self::$status;
    }

    public static function updateStatus ($newStatus)
    {
        if (!is_array ($newStatus)) return;
        self::$status = $newStatus;
    }

    public static function writeStatus()
    {
        if (!is_array (self::$status)) return;
        RemotePCControl::storeDataToJson (self::$statusFile, self::$status);
    }

    public static function updateAndWriteStatus ($newStatus)
    {
        self::updateStatus ($newStatus);
        self::writeStatus();
    }
}

interface IAction
{
    public function sent_data_valid(); // boolean, called only when write() is needed
    public function write (&$db); // mixed, string = error, boolean = true if written to db
    public function read  (&$db); // array ('response' => 'json_serializable_object', 'dbedited' => true)
    public function canWrite ($db);
    public function getWhyCantWrite();
}

class WriteWhenNeededHelper
{
    public function _canWrite ($name)
    {
        $_path = RemotePCControl::getProperDataDirPath() . "{$name}-sent-img.jpg";
        if (file_exists ($_path))
        {
            if ((time() - filemtime ($_path)) < 90)
                return false;
            else {
                unlink ($_path);
                return true;
            }
        }
        return true;
    }

    public function getWhyCantWrite()
    {
        return "An image already exists, you may want to wait for the client to download it or just let it expire (90 seconds threshold)";
    }
}

class ScreenshotAction extends WriteWhenNeededHelper implements IAction
{
    const DBKEY = "action-screenshot";
    public function sent_data_valid()
    {
        return isset ($_GET['monitor']) && is_numeric ($_GET['monitor']) && $_GET['monitor'] >= -1;
    }

    public function write (&$db)
    {
        if (isset ($db[self::DBKEY]) && $db[self::DBKEY] != -2) return "A screenshot request is already scheduled";
        $db[self::DBKEY] = intval ($_GET['monitor']);
        return true;
    }

    public function read (&$db)
    {
        if (isset ($db[self::DBKEY]) && $db[self::DBKEY] != -2)
        {
            $old = $db[self::DBKEY];
            $db[self::DBKEY] = -2;
            return array ('response' => $old, 'dbedited' => true);
        }
        return array ('response' => -2, 'dbedited' => false);
    }

    public function canWrite ($db)
    {
        return parent::_canWrite ("screenshot");
    }
}

class WebcamImgAction extends WriteWhenNeededHelper implements IAction
{
    const DBKEY = "action-webcamimg";
    public function sent_data_valid()
    {
        return true;
    }

    public function write (&$db)
    {
        if (isset ($db[self::DBKEY]) && $db[self::DBKEY]) return "A webcam image request is already scheduled";
        $db[self::DBKEY] = true;
        return true;
    }

    public function read (&$db)
    {
        if (isset ($db[self::DBKEY]) && $db[self::DBKEY])
        {
            $old = $db[self::DBKEY];
            $db[self::DBKEY] = false;
            return array ('response' => $old, 'dbedited' => true);
        }
        return array ('response' => false, 'dbedited' => true);
    }

    public function canWrite ($db)
    {
        return parent::_canWrite ("webcam");
    }
}
if (!isset ($_GET['action']) || $_GET['action'] != 'uploaded')
    header ("Content-type: text/plain");
if (RemotePCControl::getIsInProductionEnv())
    error_reporting (0);
// sanitization checks (don't edit this! :()
@array_walk (RemotePCControl::getPasswords(), function ($val, $key) {
    if (strlen ($val) != 40 || trim ($val) == '')
        RemotePCControl::fatalError ("Incorrect password hash at position {$key}");
});
if (count (RemotePCControl::getEnabledActions()) == 0)
    RemotePCControl::fatalError ("You didn't enable any action!");
RemotePCControl::initPrivateDir();
?>
