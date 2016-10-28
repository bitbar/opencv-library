import io.appium.java_client.TouchAction;
import library.AkazeImageFinder;
import library.ImageRecognition;

import org.opencv.core.Point;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.interactions.touch.TouchActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dtos.ImageRecognitionSettingsDTO;
import dtos.ImageSearchDTO;

import java.io.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created by testdroid on 22/07/16.
 */
public class TestdroidImageRecognition extends AbstractAppiumTest {

    public Logger logger = LoggerFactory.getLogger(TestdroidImageRecognition.class);
    
    AkazeImageFinder imageFinder = new AkazeImageFinder();

    private String queryimageFolder = "";
    public boolean found = false;


    //If this method is called inside a test the script will check if the device has a resolution lower than 500x500 and if so will use
    // a different set of images when trying to find a image. These images are located in /queryimages/low_res
    public void setQueryImageFolder() {
        Dimension size = driver.manage().window().getSize();
        log("Screen size: " + size.toString());
        if ((size.getHeight() <= 500) || (size.getWidth() <= 500)) {
            queryimageFolder = "low_res/";
        }
    }


    /**
     * ======================================================================================
     * FINDING AN IMAGE ON SCREEN
     * ======================================================================================
     */


    public Point[] findImageOnScreen(String image, ImageRecognitionSettingsDTO settings) throws Exception {
    	ImageSearchDTO dto = findImageOnScreen2(image, settings);
    	return dto.getImageRectangle();
    }
    
    public ImageSearchDTO findImageOnScreen2(String image, ImageRecognitionSettingsDTO settings) throws Exception {      
    	ImageSearchDTO foundImage = findImageLoop(image, settings);
        if (foundImage.isFound() && settings.isCrop()) {
        	Point[] imgRect = foundImage.getImageRectangle();
            Point top_left = imgRect[0];
            Point top_right = imgRect[1];
            Point bottom_left = imgRect[2];
            Point center = imgRect[4];
            imageFinder.cropImage(foundImage.getScreenshotFile(), top_left.x, top_left.y, top_right.x - top_left.x, bottom_left.y - top_left.y);
        }
        return foundImage;
    }

	private ImageSearchDTO findImageLoop(String image, ImageRecognitionSettingsDTO settings) throws InterruptedException, IOException, Exception {
		long start_time = System.nanoTime();
		ImageSearchDTO foundImageDto = new ImageSearchDTO();
		for (int i = 0; i < settings.getRetries(); i++) {
			// queryImageFolder is "", unless set by setQueryImageFolder()
            String queryImageFile = "queryimages/" + queryimageFolder + image + "_screenshot";
            String screenshotFile = takeScreenshot(image + "_screenshot");
            Point[] imgRect = ImageRecognition.findImage(queryImageFile, screenshotFile, settings, platformName, getScreenSizeADB());
            if (imgRect!=null){
            	long end_time = System.nanoTime();
                int difference = (int) ((end_time - start_time) / 1e6 / 1000);
                log("==> Find image took: " + difference + " secs.");
                
                foundImageDto.setImageRectangle(imgRect);
                foundImageDto.setScreenshotFile(screenshotFile);
                return foundImageDto;
            }
            retryWait(settings);
		}
		log("==> Image not found");
		return foundImageDto;
	}

	private void retryWait(ImageRecognitionSettingsDTO settings) throws InterruptedException {
		if (settings.getRetryWaitTime() > 0) {
		    log("retryWait given, sleeping " + settings.getRetryWaitTime() + " seconds.");
		    sleep(settings.getRetryWaitTime());
		}
	}

    public Point[] findImageOnScreen(String image) throws Exception {
    	ImageRecognitionSettingsDTO defaultSettings = new ImageRecognitionSettingsDTO();
    	return findImageOnScreen(image, defaultSettings);
    }

    //Searches for an image until it disappears from the current view. Good for checking if a loading screen has disappeared.
    public void waitForImageToDisappearFromScreen(String image) throws Exception {
        boolean first_time = true;
        boolean check = true;
        long start, present;
        start = System.nanoTime();
        present = start;

        log("==> Trying to find image: " + image);

        while ((check) && ((present - start) / 1e6 / 1000 < 300)) {

            if (first_time) {
                first_time = false;
                takeScreenshot(image + "_screenshot", true);
                if ((findImage(image, image + "_screenshot" + getRetryCounter())) == null) {
                    log("Loading screen not found. Moving on");
                    check = false;
                } else {
                    sleep(3);
                }
            } else {
                takeScreenshot(image + "_screenshot", false);
                if ((findImage(image, image + "_screenshot" + getRetryCounter())) == null) {
                    log("Loading screen not found. Moving on");
                    check = false;
                } else {
                    sleep(3);
                }
            }

            present = System.nanoTime();

            if ((present - start) / 1e6 / 1000 >= 300) {
                fail("Application takes too long to load: Stopping tests.....");
                check = false;
            }
        }
    }


    /**
     * ======================================================================================
     * ADB UTILITIES
     * ======================================================================================
     */


    //Uses adb commands to get the screen size. To be used when appium methods fail. Only works on Android devices.
    public Dimension getScreenSizeADB() throws Exception {
        log("trying to get size from adb...");
        log("------------------------------");
        if (platformName.equalsIgnoreCase("iOS")) {
            return driver.manage().window().getSize();
        } else {
            String adb = "adb";
            String[] adbCommand = {adb, "shell", "dumpsys", "window"};
            try {
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

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return null;
    }

    public boolean findDeviceTypeADB() throws Exception {
        log("trying to find device type ...");
        log("------------------------------");
        if (platformName.equalsIgnoreCase("iOS")) {
            //TO Be added
        } else {
            String adb = "adb";
            String[] adbCommand = {adb, "shell", "getprop", "ro.build.characteristics"};
            try {
                ProcessBuilder p = new ProcessBuilder(adbCommand);
                Process proc = p.start();
                InputStream stdin = proc.getInputStream();
                InputStreamReader isr = new InputStreamReader(stdin);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                String[] size = null;
                while ((line = br.readLine()) != null) {
                    if (line.contains("tablet")) {
                        return true;
                    }

                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;
    }

    //Uses adb commands to tap at relative coordinates. To be used when appium methods fail. Only works on Android devices.
    public void tapAtRelativeCoordinatesADB(double x_offset, double y_offset) throws Exception {
        if (platformName.equalsIgnoreCase("iOS")) {
            tapAtRelativeCoordinates(x_offset, y_offset);
        } else {
            Dimension size = getScreenSizeADB();
            log("Size of device as seen by ADB is - width: " + size.width + " height: " + size.height);
            String x = String.valueOf(size.width * x_offset);
            String y = String.valueOf(size.height * y_offset);
            log("ADB: x and y: " + x + ", " + y);
            String[] adbCommand = {"adb", "shell", "input", "tap", x, y};
//            String[] adbCommand = {"adb", "shell", "input", "touchscreen", "swipe", x, y, x, y, "2000"};

            try {
                ProcessBuilder p = new ProcessBuilder(adbCommand);
                Process proc = p.start();
                InputStream stdin = proc.getInputStream();
                InputStreamReader isr = new InputStreamReader(stdin);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null)
                    System.out.print(line);


                proc.waitFor();

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    //Uses adb commands to tap at coordinates. To be used when appium methods fail. Only works on Android devices.
    public void tapAtCoordinatesADB(double x, double y) throws Exception {
        String[] adbCommand;
        if (platformName.equalsIgnoreCase("iOS")) {
            tapAtCoordinates((int) x, (int) y);
        } else {
            int Xx = (int) x;
            int Yy = (int) y;
            String X = String.valueOf(Xx);
            String Y = String.valueOf(Yy);
            log("ADB: X: " + X + ", Y: " + Y);
//                        String[] adbCommand = {"adb", "shell", "input", "tap", X, Y};

            if (automationName.equalsIgnoreCase("selendroid")) {
                log("adb_shell_input_tap"); //works for 4.1.x. Will not work for 4.0.x
                adbCommand = new String[]{"adb", "shell", "input", "tap", X, Y};
                processBuilder(adbCommand);
                log("Tap done.");
            } else {
                adbCommand = new String[]{"adb", "shell", "input", "touchscreen", "swipe", X, Y, X, Y, "2000"};
                processBuilder(adbCommand);
            }
        }
    }

    public void processBuilder(String[] adbCommand) {
        try {
            found = true;
            ProcessBuilder p = new ProcessBuilder(adbCommand);
            Process proc = p.start();
            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
                System.out.print(line);

            proc.waitFor();

        } catch (Throwable t) {
            found = false;
            t.printStackTrace();
        }
    }

    /**
     * ======================================================================================
     * CROP IMAGE
     * ======================================================================================
     */

    public void findAndCropImageFromScreen(String image) throws Exception {
    	ImageRecognitionSettingsDTO settings = new ImageRecognitionSettingsDTO();
    	settings.setCrop(true);
    	findImageOnScreen(image, settings);
    }

    /**
     * ======================================================================================
     * TESSERACT GRAB TEXT FROM IMAGE
     * ======================================================================================
     */

    public String grabText(String image) throws Exception {
        findAndCropImageFromScreen(image);
        String imageInput = screenshotsFolder + getScreenshotsCounter() + "_" + image + "_screenshot" + getRetryCounter() + "_" + timeDifferenceStartTest + "sec" + ".png";
        String[] tesseractCommand = {"tesseract", imageInput, "stdout"};
        String value = "";
        try {
            ProcessBuilder p = new ProcessBuilder(tesseractCommand);
            Process proc = p.start();
            InputStream stdin = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line;
            String[] size = null;
            String[] splitLines;
            while ((line = br.readLine()) != null) {
                value += line;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return value;
    }

    /**
     * ======================================================================================
     * OTHER UTILITIES
     * ======================================================================================
     */

    //TODO: experimental method
    public Point correctAndroidCoordinates(Point appium_coord) throws Exception {

        Dimension appium_dimensions = driver.manage().window().getSize();
        int appium_screenWidth = appium_dimensions.getWidth();
        int appium_screenHeight = appium_dimensions.getHeight();

        Dimension adb_dimension = getScreenSizeADB();
        int adb_screenWidth = adb_dimension.getWidth();
        int adb_screenHeight = adb_dimension.getHeight();

        double x_offset = appium_coord.x / appium_screenWidth;
        double y_offset = appium_coord.y / appium_screenHeight;
        log("x_offset is : " + x_offset);
        log("y_offset is : " + y_offset);

        return new Point(x_offset * adb_screenWidth, y_offset * adb_screenHeight);
    }

}
