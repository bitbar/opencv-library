package library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FilenameUtils;
import org.opencv.core.Point;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dtos.ImageRecognitionSettingsDTO;
import dtos.ImageSearchDTO;

public class ImageRecognition {

	private static Logger logger = LoggerFactory.getLogger(ImageRecognition.class);
    private static AkazeImageFinder imageFinder = new AkazeImageFinder();
    private static void log(String message) {
        logger.info(message);
    }
	
	
    public static Point[] findImage(String image, String scene, String platformName, Dimension screenSize) throws Exception {
    	ImageRecognitionSettingsDTO setting = new ImageRecognitionSettingsDTO();
    	return findImage(image, scene, setting, platformName, screenSize);
    }
    
    
    //This method calls on the Akaze scripts to find the coordinates of a given image in another image.
    //The "image" parameter is the image that you are searching for
    //The "scene" parameter is the image in which we are looking for "image"
    // "tolerance" sets the required accuracy for the image recognition algorithm.
    public static Point[] findImage(String image, String scene, ImageRecognitionSettingsDTO settings, String platformName, Dimension screenSize) throws Exception {
        log("Searching for " + image);
        log("Searching in " + scene);
        Point[] imgRect = findImageUsingAkaze(image, scene, settings);

        if (imgRect != null) {
            if (platformName.equalsIgnoreCase("iOS")) {
                imgRect = scaleImageRectangleForIos(screenSize, imgRect, scene);
            }
            Point center = imgRect[4];
            if (!isPointInsideScreenBounds(center, screenSize)) {
                log("Screen size is (width, height): " + screenSize.getWidth() + ", " + screenSize.getHeight());
                log("WARNING: Coordinates found do not match the screen --> image not found.");
                imgRect = null;
            }
        }
        return imgRect;
    }



	private static Point[] findImageUsingAkaze(String image, String scene, ImageRecognitionSettingsDTO settings) {
		Point[] imgRect = new Point[0];
		try {
            imgRect = imageFinder.findImage(image, scene, settings.getTolerance());
        } catch (Exception e) {
            e.printStackTrace();
        }
		return imgRect;
	}



	private static Point[] scaleImageRectangleForIos(Dimension screenSize, Point[] imgRect, String scene) {
		Point[] imgRectScaled;
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
		        imgRectScaled = new Point[]{new Point(imgRect[0].x / 3, imgRect[0].y / 3), new Point(imgRect[1].x / 3, imgRect[1].y / 3), new Point(imgRect[2].x / 3, imgRect[2].y / 3), new Point(imgRect[3].x / 3, imgRect[3].y / 3), new Point(imgRect[4].x / 3, imgRect[4].y / 3)};
		        log("Device with Retina display rendered at x3 => coordinates have been recalculated");
		        imgRect = imgRectScaled;
		    }
		    else {
		        imgRectScaled = new Point[]{new Point(imgRect[0].x / 2, imgRect[0].y / 2), new Point(imgRect[1].x / 2, imgRect[1].y / 2), new Point(imgRect[2].x / 2, imgRect[2].y / 2), new Point(imgRect[3].x / 2, imgRect[3].y / 2), new Point(imgRect[4].x / 2, imgRect[4].y / 2)};
		        log("Device with Retina display rendered at x2 => coordinates have been recalculated");
		        imgRect = imgRectScaled;
		    }
		}
		return imgRect;
	}



	private static boolean isPointInsideScreenBounds(Point center, Dimension screenSize) {
		return !((center.x >= screenSize.width) || (center.x < 0) || (center.y >= screenSize.height) || (center.y < 0));
	}
	
	
	public static boolean hasImageDissappearedFromScreenBeforeTimeout(String imageFile,
			String screenshotsFolder, Dimension screenSize, String platformName) throws Exception {
		log("==> Trying to find image: " + imageFile);
        int retry_counter=0;
        long start = System.nanoTime();
        while (((System.nanoTime() - start) / 1e6 / 1000 < 300)) {
        	String screenshotName = parseFileName(imageFile) + "_screenshot_"+retry_counter;
			String screenShotFile = ImageRecognition.takeScreenshot(screenshotName, screenshotsFolder, platformName);
			if ((findImage(imageFile, screenShotFile, platformName, screenSize)) == null) {
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
    
    
    
    
    public static ImageSearchDTO findImageOnScreen(String imageFile, String screenshotsFolder, ImageRecognitionSettingsDTO settings, Dimension screenSize, String platformName) throws InterruptedException, IOException, Exception {
    	ImageSearchDTO foundImageDto = findImageLoop(imageFile, screenshotsFolder, settings, screenSize, platformName);
        if (foundImageDto.isFound() && settings.isCrop()) {
        	cropImage(foundImageDto);
        }
        return foundImageDto;
    }
    
	private static ImageSearchDTO findImageLoop(String imageFile, String screenshotsFolder, ImageRecognitionSettingsDTO settings, Dimension screenSize, String platformName) throws InterruptedException, IOException, Exception {
		long start_time = System.nanoTime();
		ImageSearchDTO foundImageDto = new ImageSearchDTO();
		String imageName = parseFileName(imageFile);
		for (int i = 0; i < settings.getRetries(); i++) {
			String screenshotName = imageName + "_screenshot_"+i;
			String screenshotFile = takeScreenshot(screenshotName,screenshotsFolder, platformName);
            Point[] imgRect = ImageRecognition.findImage(imageFile, screenshotFile, settings, platformName, screenSize);
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
	
	private static void cropImage(ImageSearchDTO foundImage) {
		Point[] imgRect = foundImage.getImageRectangle();
		Point top_left = imgRect[0];
		Point top_right = imgRect[1];
		Point bottom_left = imgRect[2];
		Point center = imgRect[4];
		imageFinder.cropImage(foundImage.getScreenshotFile(), top_left.x, top_left.y, top_right.x - top_left.x, bottom_left.y - top_left.y);
	}
    
	private static void retryWait(ImageRecognitionSettingsDTO settings) throws InterruptedException {
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
    public static String takeScreenshot(String screenshotName, String screenshotsFolder, String platformName) throws Exception {
    	long start_time = System.nanoTime();
    	
    	String screenshotFile = screenshotsFolder + screenshotName + ".png";
		String fullFileName = System.getProperty("user.dir") + "/" + screenshotFile;

		if (platformName.equalsIgnoreCase("iOS")) {
    		takeIDeviceScreenshot(fullFileName);
    	} else if (platformName.equalsIgnoreCase("Android")) {
    		takeAndroidScreenshot(fullFileName);
    	} else{
    		throw new Exception("Invalid platformName: "+platformName);
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


	

	private static void takeIDeviceScreenshot(String fullFileName) throws IOException, InterruptedException {
		String udid = System.getenv("UDID");
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
