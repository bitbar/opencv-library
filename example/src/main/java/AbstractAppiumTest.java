import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import objects.PlatformType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by testdroid on 22/07/16.
 */
public abstract class AbstractAppiumTest {

    public static final int SHORT_SLEEP = 1;
    public static final int MEDIUM_SLEEP = 5;
    public static final int LONG_SLEEP = 10;

    protected static Logger logger = LoggerFactory.getLogger(AbstractAppiumTest.class);



    protected static AppiumDriver<MobileElement> driver;
    protected static int defaultWaitTime = 120;

    public static String screenshotsFolder = "";
    public static String appFile = System.getenv("APP_FILE");
    public static PlatformType platform;
    public static String automationName = System.getenv("AUTOMATION_NAME");
    public static String deviceName = System.getenv("DEVICE_NAME");
    public static String udid = System.getenv("UDID");
    public static String platformVersion = System.getenv("PLATFORM_VERSION");
    // Set to false to autoDismiss
    public static boolean autoAccept = true;
    public static boolean idevicescreenshotExists = false;


    public static AppiumDriver getIOSDriver() throws Exception {
        if (appFile == null) {
            appFile = "application.ipa";
        }
        if (platform == null) {
        	platform = PlatformType.IOS;
        }
        if (deviceName == null){
        	deviceName = "device";
        }
        if (platformVersion == null){
        	platformVersion = "";
        }
        // Use default "appium" automation for iOS
        automationName = "appium";

        screenshotsFolder = "target/reports/screenshots/ios/";
        File dir = new File(screenshotsFolder);
        dir.mkdirs();

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", platform.getPlatformName());
        capabilities.setCapability("deviceName", deviceName);
        if (udid != null)
            capabilities.setCapability("udid", udid);
        if (platformVersion != null)
            capabilities.setCapability("platformVersion", platformVersion);

        capabilities.setCapability("app", System.getProperty("user.dir") + File.separator + appFile);
        capabilities.setCapability("newCommandTimeout", 120);
        capabilities.setCapability("nativeInstrumentsLib", true);
        if (autoAccept) {
            capabilities.setCapability("autoAcceptAlerts", true);
            capabilities.setCapability("autoDismissAlerts", false);
        } else {
            capabilities.setCapability("autoAcceptAlerts", false);
            capabilities.setCapability("autoDismissAlerts", true);
        }

        idevicescreenshotCheck();

        log("Creating Appium session, this may take couple minutes..");
        driver = new IOSDriver<MobileElement>(new URL("http://localhost:4723/wd/hub"), capabilities);
        driver.manage().timeouts().implicitlyWait(defaultWaitTime, TimeUnit.SECONDS);
        return driver;
    }

    public static AppiumDriver getAndroidDriver() throws Exception {
        if (appFile == null) {
            appFile = "application.apk";
        }
        if (platform == null) {
        	platform = PlatformType.ANDROID;
        }
        if (deviceName == null){
            deviceName = "device";
        }
        if (automationName == null){
        	automationName = "appium";
        }
        screenshotsFolder = "target/reports/screenshots/android/";
        File dir = new File(screenshotsFolder);
        dir.mkdirs();

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("automationName", automationName);
        capabilities.setCapability("platformName", platform.getPlatformName());
        capabilities.setCapability("deviceName", "Android Device");
        if (udid != null)
            capabilities.setCapability("udid", udid);
        if (platformVersion != null)
            capabilities.setCapability("platformVersion", platformVersion);

        capabilities.setCapability("app", System.getProperty("user.dir") + File.separator + appFile);
        capabilities.setCapability("newCommandTimeout", 120);

        log("Creating Appium session, this may take couple minutes..");
        driver = new AndroidDriver<MobileElement>(new URL("http://localhost:4723/wd/hub"), capabilities);
        driver.manage().timeouts().implicitlyWait(defaultWaitTime, TimeUnit.SECONDS);
        driver.resetApp();
        return driver;
    }



    //On a test run on the local machine this method will save the Reports folder in different folders on every test run.
    public static void savePreviousRunReports() {
        long millis = System.currentTimeMillis();
        File dir = new File("./target/reports");
        File newName = new File("./target/reports" + millis);
        if (dir.isDirectory()) {
            dir.renameTo(newName);
        } else {
            dir.mkdir();
            dir.renameTo(newName);
        }
    }

    public static boolean idevicescreenshotCheck() throws IOException, InterruptedException {
        String[] cmd = new String[]{"idevicescreenshot", "--help"};
        int exitVal = -1;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            exitVal = p.waitFor();
        } catch (IOException e) {
            log(e.toString());
        }
        if (exitVal == 0) {
            log("idevicescreenshot exited with value: " + exitVal + ". Using it for screenshots.");
            idevicescreenshotExists = true;
        } else {
            log("idevicescreenshot process exited with value: " + exitVal + ". Won't be using it for screenshots.");
            idevicescreenshotExists = false;
        }
        return idevicescreenshotExists;
    }

    //Stops the script for the given amount of seconds.
    public static void sleep(double seconds) throws Exception {
        log("Waiting for " + seconds + " sec");
        seconds = seconds * 1000;
        Thread.sleep((int) seconds);
    }

    public static void log(String message) {
        logger.info(message);
    }

    

}
