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

    private String queryImageSubFolder = "";
    public boolean found = false;


    //If this method is called inside a test the script will check if the device has a resolution lower than 500x500 and if so will use
    // a different set of images when trying to find a image. These images are located in /queryimages/low_res
    public void setQueryImageFolder() {
        Dimension size = driver.manage().window().getSize();
        log("Screen size: " + size.toString());
        if ((size.getHeight() <= 500) || (size.getWidth() <= 500)) {
            queryImageSubFolder = "low_res/";
        }
    }


    /**
     * ======================================================================================
     * FINDING AN IMAGE ON SCREEN
     * ======================================================================================
     */
    

    public Point[] findImageOnScreen(String image) throws Exception {
    	ImageRecognitionSettingsDTO defaultSettings = new ImageRecognitionSettingsDTO();
    	return findImageOnScreen(image, defaultSettings).getImageRectangle();
    }
    
    public ImageSearchDTO findImageOnScreen(String imageName, ImageRecognitionSettingsDTO settings) throws Exception { 
    	// queryImageFolder is "", unless set by setQueryImageFolder()
        String queryImageFolder = "queryimages/" + queryImageSubFolder;
        String screenshotsFolder = "target/reports/screenshots/"; //TODO Severi remove this
        String imageFile = queryImageFolder+imageName;
        log("Searching for: "+imageFile);
    	ImageSearchDTO foundImage = ImageRecognition.findImageOnScreen(imageFile, screenshotsFolder, settings, getScreenSize(), platformName);
        return foundImage;
    }

    public void waitForImageToDisappearFromScreen(String image) throws Exception {
        String queryImageFolder = "queryimages/" + queryImageSubFolder; //TODO Severi remove this
        String screenshotsFolder = "target/reports/screenshots/"; //TODO Severi remove this
        Dimension screenSize = getScreenSize(); //TODO Severi remove this
        String imageFile = queryImageFolder+image;
		boolean hasImageDisappeared = ImageRecognition.hasImageDissappearedFromScreenBeforeTimeout(imageFile, screenshotsFolder, screenSize, platformName);
		assert(hasImageDisappeared);
    }





    /**
     * ======================================================================================
     * ADB UTILITIES
     * ======================================================================================
     */


    //Uses adb commands to get the screen size. To be used when appium methods fail. Only works on Android devices.
    public Dimension getScreenSize() throws Exception {
        log("trying to get size from adb...");
        log("------------------------------");
        if (platformName.equalsIgnoreCase("iOS")) {
            return driver.manage().window().getSize();
        } else {
            return getAndroidScreenSize();
        }
    }


	private Dimension getAndroidScreenSize() throws IOException, InterruptedException {
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
    	ImageSearchDTO imageSearch = findImageOnScreen(image, settings);
        String text = ImageRecognition.getTextStringFromImage(imageSearch.getScreenshotFile());
		return text;
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

        Dimension adb_dimension = getScreenSize();
        int adb_screenWidth = adb_dimension.getWidth();
        int adb_screenHeight = adb_dimension.getHeight();

        double x_offset = appium_coord.x / appium_screenWidth;
        double y_offset = appium_coord.y / appium_screenHeight;
        log("x_offset is : " + x_offset);
        log("y_offset is : " + y_offset);

        return new Point(x_offset * adb_screenWidth, y_offset * adb_screenHeight);
    }

}
