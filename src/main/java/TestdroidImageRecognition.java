import library.AkazeImageFinder;
import library.ImageRecognition;

import org.apache.commons.io.FileUtils;
import org.opencv.core.Point;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dtos.ImageRecognitionSettingsDTO;
import dtos.ImageSearchDTO;

import java.io.*;


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
    	ImageSearchDTO foundImage = findImageLoop(image, settings, getScreenSizeADB());
        if (foundImage.isFound() && settings.isCrop()) {
        	cropImage(foundImage);
        }
        return foundImage;
    }


	private void cropImage(ImageSearchDTO foundImage) {
		Point[] imgRect = foundImage.getImageRectangle();
		Point top_left = imgRect[0];
		Point top_right = imgRect[1];
		Point bottom_left = imgRect[2];
		Point center = imgRect[4];
		imageFinder.cropImage(foundImage.getScreenshotFile(), top_left.x, top_left.y, top_right.x - top_left.x, bottom_left.y - top_left.y);
	}

	private ImageSearchDTO findImageLoop(String image, ImageRecognitionSettingsDTO settings, Dimension screenSize) throws InterruptedException, IOException, Exception {
		long start_time = System.nanoTime();
		ImageSearchDTO foundImageDto = new ImageSearchDTO();
		for (int i = 0; i < settings.getRetries(); i++) {
			// queryImageFolder is "", unless set by setQueryImageFolder()
            String queryImageFile = "queryimages/" + queryimageFolder + image;
            String screenshotFile = takeScreenshot(image + "_screenshot");
            Point[] imgRect = ImageRecognition.findImage(queryImageFile, screenshotFile, settings, platformName, screenSize);
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

    public boolean waitForImageToDisappearFromScreen(String image) throws Exception {
        long start = System.nanoTime();
        log("==> Trying to find image: " + image);
        int retry_counter=0;
        String queryImageFile = "queryimages/" + queryimageFolder + image;
        Dimension screenSize = getScreenSizeADB();
        
        while (((System.nanoTime() - start) / 1e6 / 1000 < 300)) {
        	String screenShotFile = takeScreenshot(image + "_screenshot_"+retry_counter);
			if ((ImageRecognition.findImage(queryImageFile, screenShotFile, platformName, screenSize)) == null) {
        		log("Image has successfully disappeared from screen.");
        		return true;
        	}
			sleep(3);			
        }
        logger.warn("Image did not disappear from screen");
        return false;
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

    public String grabTextFromImage(String image) throws Exception {
    	ImageRecognitionSettingsDTO settings = new ImageRecognitionSettingsDTO();
    	settings.setCrop(true);
    	ImageSearchDTO imageSearch = findImageOnScreen2(image, settings);
        String text = ImageRecognition.getTextStringFromImage(imageSearch.getScreenshotFile());
		return text;
    }
    
    
    public String takeScreenshot(String screenshotName) throws IOException, InterruptedException {
    	if (idevicescreenshotExists) {
    		// Keep Appium session alive between multiple non-driver screenshots
    		driver.manage().window().getSize();
    	}

    	long start_time = System.nanoTime();
    	String screenshotFile = screenshotsFolder + screenshotName + ".png";
		String fullFileName = System.getProperty("user.dir") + "/" + screenshotFile;

    	if (platformName.equalsIgnoreCase("iOS") && idevicescreenshotExists) {
    		takeIDeviceScreenshot(fullFileName);
    	} else {
    		takeAppiumScreenshot(fullFileName);
    	}
    	long end_time = System.nanoTime();
    	int difference = (int) ((end_time - start_time) / 1e6 / 1000);
    	logger.info("==> Taking a screenshot took " + difference + " secs.");
    	return screenshotFile;
	}

	private void takeAppiumScreenshot(String fullFileName) {
		File scrFile = driver.getScreenshotAs(OutputType.FILE);
		try {
			File testScreenshot = new File(fullFileName);
			FileUtils.copyFile(scrFile, testScreenshot);
			logger.info("Screenshot stored to {}", testScreenshot.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void takeIDeviceScreenshot(String fullFileName) throws IOException, InterruptedException {
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
