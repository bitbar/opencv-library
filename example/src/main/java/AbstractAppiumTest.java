import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import objects.PlatformType;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public abstract class AbstractAppiumTest {

    protected static Logger logger = LoggerFactory.getLogger(AbstractAppiumTest.class);
    protected static AppiumDriver<MobileElement> driver;
    protected static int defaultWaitTime = 120;

    public static String appFile = System.getenv("APP_FILE");
    public static PlatformType platform;
    public static String automationName = System.getenv("AUTOMATION_NAME");
    public static String deviceName = System.getenv("DEVICE_NAME");
    public static String udid = System.getenv("UDID");
    public static String platformVersion = System.getenv("PLATFORM_VERSION");
    // Set to false to autoDismiss
    public static boolean autoAccept = true;

    public static AppiumDriver<MobileElement> getIOSDriver() throws Exception {
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

        log("Creating Appium session, this may take couple minutes..");
        driver = new IOSDriver<MobileElement>(new URL("http://localhost:4723/wd/hub"), capabilities);
        driver.manage().timeouts().implicitlyWait(defaultWaitTime, TimeUnit.SECONDS);
        return driver;
    }

    public static AppiumDriver<MobileElement> getAndroidDriver() throws Exception {
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

    public static void log(String message) {
        logger.info(message);
    }

}
