package imagerecognition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FilenameUtils;
import org.opencv.core.Point;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import objects.ImageLocation;
import objects.ImageRecognitionSettings;
import objects.ImageSearchResult;
import objects.PlatformType;

public class ImageRecognition {

    private static Logger logger = LoggerFactory.getLogger(ImageRecognition.class);
    private static AkazeImageFinder imageFinder = new AkazeImageFinder();
    private static void log(String message) {
        logger.info(message);
    }

    public static void setUp(){
        AkazeImageFinder.setupOpenCVEnv();
    }

    // TODO remove this and make private when a way is found to get the screen size for iOS without the appium driver
    // and then remove the screen size parameter from other methods
    public static Dimension getScreenSize(PlatformType platform, AppiumDriver<MobileElement> driver) throws Exception {
        if (platform.equals(PlatformType.IOS)) {
            return driver.manage().window().getSize();
        } else {
            return getAndroidScreenSize();
        }
    }

    private static Dimension getAndroidScreenSize() throws IOException, InterruptedException {
        String adb = "adb";
        String[] adbCommand = {adb, "shell", "dumpsys", "window"};
        ProcessBuilder p = new ProcessBuilder(adbCommand);
        Process proc = p.start();
        InputStream stdin = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdin);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        String[] size = null;
        while ((line = br.readLine()) != null) {
            if (!line.contains("OriginalmUnrestrictedScreen")) { //we do this check for devices with android 5.x+ The adb command returns an extra line with the values 0x0 which must be filtered out.
                if (line.contains("mUnrestrictedScreen")) {
                    proc.waitFor();
                    String[] tmp = line.split("\\) ");
                    size = tmp[1].split("x");
                }
            }
        }
        int width = Integer.parseInt(size[0]);
        int height = Integer.parseInt(size[1]);
        Dimension screenSize = new Dimension(width, height);
        return screenSize;
    }


    public static ImageLocation findImage(String image, String scene, PlatformType platform, Dimension screenSize) throws Exception {
        ImageRecognitionSettings setting = new ImageRecognitionSettings();
        return findImage(image, scene, setting, platform, screenSize);
    }


    //This method calls on the Akaze scripts to find the coordinates of a given image in another image.
    //The "image" parameter is the image that you are searching for
    //The "scene" parameter is the image in which we are looking for "image"
    // "tolerance" sets the required accuracy for the image recognition algorithm.
    public static ImageLocation findImage(String image, String scene, ImageRecognitionSettings settings, PlatformType platform, Dimension screenSize) throws Exception {
        log("Searching for " + image);
        log("Searching in " + scene);
        ImageLocation imgLocation = findImageUsingAkaze(image, scene, settings);

        if (imgLocation != null) {
            if (platform.equals(PlatformType.IOS)) {
                imgLocation = scaleImageRectangleForIos(screenSize, imgLocation, scene);
            }
            Point center = imgLocation.getCenter();
            if (!isPointInsideScreenBounds(center, screenSize)) {
                log("Screen size is (width, height): " + screenSize.getWidth() + ", " + screenSize.getHeight());
                log("WARNING: Coordinates found do not match the screen --> image not found.");
                imgLocation = null;
            }
        }
        return imgLocation;
    }



    private static ImageLocation findImageUsingAkaze(String image, String scene, ImageRecognitionSettings settings) {
        ImageLocation location = imageFinder.findImage(image, scene, settings.getTolerance());
        return location;
    }



    private static ImageLocation scaleImageRectangleForIos(Dimension screenSize, ImageLocation imageLocation, String scene) {
        //for retina devices we need to recalculate coordinates
        double sceneHeight = imageFinder.getSceneHeight(scene);
        double sceneWidth = imageFinder.getSceneWidth(scene);
        int screenHeight = screenSize.getHeight();
        int screenWidth = screenSize.getWidth();

        // Make sure screenshot size values are "landscape" for comparison
        if (sceneHeight > sceneWidth) {
            double temp = sceneHeight;
            sceneHeight = sceneWidth;
            sceneWidth = temp;
        }

        // Make sure screen size values are "landscape" for comparison
        if (screenHeight > screenWidth) {
            int temp = screenHeight;
            screenHeight = screenWidth;
            screenWidth = temp;
        }

        if ((screenHeight<sceneHeight) && (screenWidth<sceneWidth)) {
            if ((screenHeight<sceneHeight/2)&&(screenWidth<sceneWidth/2)) {
                log("Recalculating coordinates for x3 retina displays..");
                imageLocation.divideCoordinatesBy(3);
                log("Device with Retina display rendered at  => coordinates have been recalculated");
            }
            else {
                log("Recalculating coordinates for x2 retina displays..");
                imageLocation.divideCoordinatesBy(2);
                log("Device with Retina display rendered at x2 => coordinates have been recalculated");
            }
        }
        return imageLocation;
    }



    private static boolean isPointInsideScreenBounds(Point center, Dimension screenSize) {
        return !((center.x >= screenSize.width) || (center.x < 0) || (center.y >= screenSize.height) || (center.y < 0));
    }


    public static boolean hasImageDissappearedFromScreenBeforeTimeout(String imageFile,
            String screenshotsFolder, Dimension screenSize, PlatformType platform) throws Exception {
        log("==> Trying to find image: " + imageFile);
        int retry_counter=0;
        long start = System.nanoTime();
        while (((System.nanoTime() - start) / 1e6 / 1000 < 300)) {
            String screenshotName = parseFileName(imageFile) + "_screenshot_"+retry_counter;
            String screenShotFile = ImageRecognition.takeScreenshot(screenshotName, screenshotsFolder, platform);
            if ((findImage(imageFile, screenShotFile, platform, screenSize)) == null) {
                log("Image has successfully disappeared from screen.");
                return true;
            }
            sleep(3);	
            retry_counter++;
        }
        logger.warn("Image did not disappear from screen");
        return false;
    }

    private static String parseFileName(String imageFile){
        return FilenameUtils.getBaseName(imageFile);
    }



    public static String getTextStringFromImage(String imageInput) {
        String[] tesseractCommand = {"tesseract", imageInput, "stdout"};
        String value = "";
        try {
            ProcessBuilder p = new ProcessBuilder(tesseractCommand);
            Process proc = p.start();
            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                value += line;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return value;
    }




    public static ImageSearchResult findImageOnScreen(String imageFile, String screenshotsFolder, ImageRecognitionSettings settings, Dimension screenSize, PlatformType platform) throws InterruptedException, IOException, Exception {
        ImageSearchResult foundImageDto = findImageLoop(imageFile, screenshotsFolder, settings, screenSize, platform);
        if (foundImageDto.isFound() && settings.isCrop()) {
            cropImage(foundImageDto);
        }
        return foundImageDto;
    }

    private static ImageSearchResult findImageLoop(String imageFile, String screenshotsFolder, ImageRecognitionSettings settings, Dimension screenSize, PlatformType platform) throws InterruptedException, IOException, Exception {
        long start_time = System.nanoTime();
        ImageSearchResult foundImageDto = new ImageSearchResult();
        String imageName = parseFileName(imageFile);
        for (int i = 0; i < settings.getRetries(); i++) {
            String screenshotName = imageName + "_screenshot_"+i;
            String screenshotFile = takeScreenshot(screenshotName,screenshotsFolder, platform);
            ImageLocation imageLocation = ImageRecognition.findImage(imageFile, screenshotFile, settings, platform, screenSize);
            if (imageLocation!=null){
                long end_time = System.nanoTime();
                int difference = (int) ((end_time - start_time) / 1e6 / 1000);
                log("==> Find image took: " + difference + " secs.");
                foundImageDto.setImageLocation(imageLocation);
                foundImageDto.setScreenshotFile(screenshotFile);
                return foundImageDto;
            }
            retryWait(settings);
        }
        log("==> Image not found");
        return foundImageDto;
    }

    private static void cropImage(ImageSearchResult foundImage) {
        log("Cropping image..");
        imageFinder.cropImage(foundImage);
        log("Cropping image.. Succeeded!");
    }

    private static void retryWait(ImageRecognitionSettings settings) throws InterruptedException {
        if (settings.getRetryWaitTime() > 0) {
            log("retryWait given, sleeping " + settings.getRetryWaitTime() + " seconds.");
            sleep(settings.getRetryWaitTime());
        }
    }

    //Stops the script for the given amount of seconds.
    private static void sleep(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000);
    }




    /* TODO
     *     	if (idevicescreenshotExists) {
    		// Keep Appium session alive between multiple non-driver screenshots
    		driver.manage().window().getSize();
    	}

     */
    public static String takeScreenshot(String screenshotName, String screenshotsFolder, PlatformType platform) throws Exception {
        long start_time = System.nanoTime();

        String screenshotFile = screenshotsFolder + screenshotName + ".png";
        String fullFileName = System.getProperty("user.dir") + "/" + screenshotFile;

        if (platform.equals(PlatformType.IOS)) {
            takeIDeviceScreenshot(fullFileName);
        } else if (platform.equals(PlatformType.ANDROID)) {
            takeAndroidScreenshot(fullFileName);
        } else{
            throw new Exception("Invalid platformType: "+platform);
        }

        long end_time = System.nanoTime();
        int difference = (int) ((end_time - start_time) / 1e6 / 1000);
        logger.info("==> Taking a screenshot took " + difference + " secs.");
        return screenshotFile;
    }


    private static void takeAndroidScreenshot(String fullFileName) throws IOException, InterruptedException {
        log("Taking android screenshot...");
        log(fullFileName);
        String[] cmd = new String[]{"screenshot2", "-d", fullFileName};
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = in.readLine()) != null)
            log(line);

        int exitVal = p.waitFor();
        if (exitVal != 0) {
            log("screenshot2 process exited with value: " + exitVal);
        }
    }




    private static void takeIDeviceScreenshot(String fullFileName) throws Exception {
        String udid = System.getenv("UDID");
        if (udid==null){
            throw new Exception("$UDID was null, set UDID environment variable and try again");
        }
        String[] cmd = new String[]{"idevicescreenshot", "-u", udid, fullFileName};
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = in.readLine()) != null)
            log(line);

        int exitVal = p.waitFor();
        if (exitVal != 0) {
            log("idevicescreenshot process exited with value: " + exitVal);
        }
        cmd = new String[]{"sips", "-s", "format", "png", fullFileName, "--out", fullFileName};
        p = Runtime.getRuntime().exec(cmd);
        exitVal = p.waitFor();
        if (exitVal != 0) {
            log("sips process exited with value: " + exitVal);
        }
    }




}
