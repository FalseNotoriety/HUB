import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import spark.embeddedserver.NotSupportedException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Utils {

    // Logging
    static String SERVER_LOG = "hub_server_log.txt";
    static String CLIENT_LOG = "hub_client_log.txt";

    static String STARTUP_SCRIPT = "Creating startup script...";

    // Command
    static String CMD = "Command";
    static String CMD_NOTAUTH = "Not authorized to run the command";
    static String CMD_RECV = "Command received";
    static String CMD_RUN = "Running";
    static String CMD_NOTFOUND = "not Found";
    static String CMD_INVALID = "Command Invalid";
    static String CMD_ARGS_INVALID = "Arguments Invalid";
    static String CMD_EXISTS = " duplicate command";
    static String CMD_ADD = "Add";
    static String CMD_LVL = "level";

    // Module
    static String MODULE = "Module";
    static String MODULE_INIT = "Initialized";
    static String MODULE_OK = "[ OK ]";
    static String MODULE_FAIL = "[FAIL]";
    static String MODULE_LOAD_COMPLETE = "MODULES LOADED";

    // Loading
    static String LOADING = "LOADING";

    // Network
    static String NETWORK_LOCALHOST = "127.0.0.1";
    static String NETWORK_REQ_ERROR = "REQUEST_ERROR";
    static String NETWORK_CONNECTION_TIMEOUT = "TIMEOUT";
    static String NETWORK_CONNECTION_REFUSED = "CONNECTION_REFUSED";
    static String NETWORK_GENERAL_FAILURE = "FAILURE";
    static String NETWORK_UNAUTHORIZED = "UNAUTHORIZED";
    static String NETWORK_UNKNOWN_IP = "UNKNOWN IP";
    static String NETWORK_UNKNOWN_MAC = "UNKNOWN MAC";

    static String AUTH_PASS = "Authentication valid";
    static String AUTH_FAIL = "Authentication invalid";
    static String CONTACTING_HUB = "Contacting HUB to register";
    static String CONNECTION_RETRY = "Retrying connection in";
    static String CONNECTION_LOST = "Connection to HUB lost.";
    static String CONNECTION_RESTORED = "Connection restored.";
    static String CONNECTION_READY = "Connected to HUB. Awaiting commands";
    static String CONNECTION_FAILED = "Registration failed, could not contact HUB.";

    static int DEFAULT_PORT = 5000;

    // Device
    static String SERVER_NAME = "HUB";
    static String SERVER_TITLE = "HUB SERVER";
    static String CLIENT_TITLE = "HUB CLIENT";
    static String DEVICE_UNKNOWN = "Unknown";
    static String DEVICE_NOTFOUND = "Device '%s' Not Found";
    static String DEVICE_COULDNOT_CONTACT = "Could not contact '%s'";
    static String DEVICE_ONLINE = "Online";
    static String DEVICE_OFFLINE = "Offline";
    static String DEVICE_ALREADY_REGISTERED = "Device '%s' already registered";
    static String DEVICE_REGISTERED = "Registered device '%s'";
    static String DEVICE_UNREGISTERED = "Device '%s' unregistered";

    static String DEVICE_SHUTDOWN = "Shutting down %s";
    static String DEVICE_RESTART = "Restarting %s";
    static String DEVICE_HIBERNATING = "Hibernating %s";
    static String DEVICE_LOGOFF = "Logging off %s";
    static String DEVICE_WOL = "Sending magic packet to %s";

    // Error
    static String ERROR = "Error:";
    static String ERR_CMD_OS_NOTSUPPORTED = "Could not run command, OS does not support it";
    static String ERR_CMD_UNKNWN = "Could not run command, unknown cause";
    static String ERR_CMD_WOL = "Could not send wake on lan packet";



    // Supported operating systems
    enum OS_TYPE {
        WIN,
        UNIX,
        UNSUPPORTED
    }


    // Clearance levels
    enum CLEARANCE { // IMPORTANT: MUST BE IN INCREASING ORDER - LOWER LEVELS ARE INHERITED
        HIDDEN,
        NONE,
        BASIC,
        FULL
    }


    // AUTHORIZATION //
    public static String bhash(String key){
        int r = 0;
        for (int i = 0; i < key.length(); i++){
            r += (i+1) * key.charAt(i);
        }
        return String.valueOf(r);
    }


    // PRINTING //
    public static void printMsg(String message){ System.out.println(message); }
    public static void printMsg(String[] messages, String header){
        StringBuilder sb = new StringBuilder();
        for (String msg : messages){
            sb.append(msg + " ");
        }

        if (header.length() > 0){
            System.out.println(header + ":" + sb.toString());
        } else {
            System.out.println(sb.toString());
        }

    }

    public static void logMsg(String message, boolean display, String fileName){ logMsg(new String[]{message}, display, fileName); }
    public static void logMsg(String[] messages, boolean display, String fileName){
        // Assemble message
        StringBuilder sb = new StringBuilder();
        for (String msg : messages){
            sb.append(msg.replace("\n", "\\n") + " "); //Replace newline with literal newline
        }

        // Get timestamp
        String timeStamp = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(new Date());

        // Assemble message
        String msg = timeStamp + sb.toString();

        // Display
        if (display){
            printMsg(msg);
        }

        // Get home directory
        String UserHome = System.getProperty("user.home");

        // Try to write
        if (fileName != null && fileName.length() > 0) {
            FileWriter fw = null;
            BufferedWriter bw = null;
            try {
                File logFile = new File(UserHome + "\\" + fileName);

                // Create file if it DNE
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }

                // Write to log
                fw = new FileWriter(logFile.getAbsoluteFile(), true);
                bw = new BufferedWriter(fw);

                // Write
                bw.write(msg + "\n");
            } catch (IOException ex) {
                System.out.println(String.format("Error: Could not write to logfile '%s':%s", fileName, ex.getMessage()));
            } finally {
                try {
                    if (bw != null) bw.close();
                    if (fw != null) fw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void printHeader(String title, String deviceName, String deviceIP, String deviceMAC){
        int numSlashes = 10;

        //Program info
        String m_creator = "Zach Lerew 2016-17";

        // Assemble sub messages
        List<String> rows = Arrays.asList(title, m_creator, "", deviceName, deviceIP, deviceMAC);
        int maxLen = rows.get(0).length();
        for (String s : rows) { if (s.length() > maxLen) { maxLen = s.length(); } }

        // Print top row of slashes
        System.out.printf("%s\n", String.format("%" + String.valueOf(maxLen + numSlashes*2 + 2) + "s", "/").replace(' ', '/'));

        // Pad the messages
        int offset;
        for (String str : rows){
            offset = (int)Math.floor( (maxLen - str.length())/2 ) + 1; //+1 to avoid offset=0
            System.out.printf("%s%s%s%s%s\n",
                    String.format("%" + String.valueOf(numSlashes) + "s", "/").replace(' ', '/'),
                    String.format("%" + String.valueOf(offset) + "s", " "),
                    str,
                    String.format("%" + String.valueOf(offset + (str.length()%2!=0?1:0)) + "s", " "),
                    String.format("%" + String.valueOf(numSlashes) + "s", "/").replace(' ', '/')
            );
        }

        // Print bottom row of slashes
        System.out.printf("%s\n", String.format("%" + String.valueOf(maxLen + numSlashes*2 + 2) + "s", "/").replace(' ', '/'));
    }


    // DEVICE EXECUTION //
    public static void ExecuteBackgroundTask(final Runnable runnable, final int delay){
        //final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        //executor.schedule(runnable, delay, TimeUnit.MILLISECONDS);


        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(new Runnable() {
            @Override public void run() {
                runnable.run();
            }
        }, delay, TimeUnit.MILLISECONDS);

/*
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                System.out.println("BG Task");
                runnable.run();
            }
        });
        thread.start();


        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("BG Timer Task");
                        runnable.run();
                    }
                }, delay);
            }
        });
        thread.start();
*/
    }


    // NETWORK //
    public static HttpResponse<String> SendCommand(String Command, String[] Arguments, String IP, int Port, String Username, String Password) {
        String URL = "http://" + IP + ":" + String.valueOf(Port);
        HttpResponse<String> response = null;

        try {
            String auth = Username + ":" + Password;

            JSONArray ja = new JSONArray(Arguments);
            Unirest.setTimeouts(1000, 1000);
            response = Unirest.post(URL)
                    .header("connection", "close")
                    .header("Authorization", new String(Base64.encodeBase64(auth.getBytes())))
                    .field("cmd", Command)
                    .field("args", ja.toString())
                    .asString();

            /* Example of a similar GET request
            response = Unirest.get(URL)
                    .header("connection", "close")
                    .queryString("auth", String.valueOf(bhash(Token)))
                    .queryString("cmd", Command)
                    .queryString("args", new ArrayList<>(Arrays.asList(Arguments)))
                    .asString();
            */


        } catch (UnirestException ex){
            //System.out.println("SEND COMMAND ERROR " + ex.getMessage());

            // timeout
            if (ex.getMessage().contains("ConnectTimeoutException")){
                //return NETWORK_REQ_ERROR + ":" + NETWORK_CONNECTION_TIMEOUT;
            }
            // refused
            if (ex.getMessage().contains("HttpHostConnectException")) {
                //return NETWORK_REQ_ERROR + ":" + NETWORK_CONNECTION_REFUSED;
            }
            // other failure
            //return NETWORK_REQ_ERROR + ":" + NETWORK_GENERAL_FAILURE + "\n    Reason:" + ex.getMessage();
        }

        return response;
    }


    // STARTUP //
    public static void CreateStartupScript(OS_TYPE OS, String filename, String[] arguments) throws IOException, UnsupportedOperationException{
        logMsg(STARTUP_SCRIPT, true, null);

        // Assemble startup file contents
        StringBuilder contents = new StringBuilder();
        // Startup directory
        String startupDirectory;

        // PWD
        String PWD = System.getProperty("user.dir");
        String UserHome = System.getProperty("user.home");

        // Startup script file
        File startupScript = null;

        // OS specific startup data
        if (OS == Utils.OS_TYPE.WIN) {
            // Windows unique strings
            startupDirectory = String.format("%s\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\", UserHome);
            //startupDirectory = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
            contents.append(String.format("java -jar \"%s\\%s.jar\" ", PWD, filename));
            for (String s : arguments){ contents.append(s + " "); }
            contents.append("\r\npause");

            // Remove old script if it exists
            //Runtime.getRuntime().exec(String.format("del %s%s.bat", startupDirectory, filename));

            startupScript = new File(startupDirectory, filename + ".bat");

            // Create batch script file
            //Runtime.getRuntime().exec(String.format("echo. > \"%s%s.bat\"", startupDirectory, filename));

            // Write contents to startup file
            //Runtime.getRuntime().exec(String.format("echo \"%s\" > \"%s%s.bat\"", contents, startupDirectory, filename));
        } else if (OS == Utils.OS_TYPE.UNIX){
            // Unix unique strings
            startupDirectory = "/etc/init.d/";
            contents.append(String.format("#!/bin/sh\n"));
            contents.append(String.format("sudo java -jar %s/%s.jar ", PWD, filename));
            for (String s : arguments){ contents.append(s + " "); }

            // Remove old script if it exists
            //Runtime.getRuntime().exec(String.format("sudo rm -f %s%s.sh", startupDirectory, filename));

            startupScript = new File(startupDirectory, filename + ".sh");

            //Runtime.getRuntime().exec(String.format("sudo printf '%s' | sudo tee ./%s.sh", contents, filename));

            // Make script executable
            Runtime.getRuntime().exec(String.format("sudo chmod +x %s%s.sh", startupDirectory, filename));

            // Register script to run on startup
            Runtime.getRuntime().exec(String.format("sudo update-rc.d %s.sh defaults", filename));

            // Link rc0.d to init.d
            //Runtime.getRuntime().exec(String.format("sudo ln -s %s%s.sh /etc/rc0.d/%s.sh", startupDirectory, filename, filename));
        } else {
            throw new UnsupportedOperationException("This operating system is not supported");
        }

        // Try to write to startup script
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(startupScript.getAbsoluteFile(), false));
            try {
                startupScript.createNewFile();
                bw.write(contents.toString());
            } catch (IOException ex) {
                System.out.println("Error:Could not create startup script");
            } finally {
                bw.close();
            }
        } catch (Exception ex){
            System.out.println(String.format("Error:Could not create startup script. Did you run this with elevated permissions?: \n%s", ex.getMessage()));
        }

    }
}
