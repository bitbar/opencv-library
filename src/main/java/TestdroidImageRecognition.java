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
    private String queryImageSubFolder = "";


    //If this method is called inside a test the script will check if the device has a resolution lower than 500x500 and if so will use
    // a different set of images when trying to find a image. These images are located in /queryimages/low_res
    public void setQueryImageFolder() {
        Dimension size = driver.manage().window().getSize();
        log("Screen size: " + size.toString());
        if ((size.getHeight() <= 500) || (size.getWidth() <= 500)) {
            queryImageSubFolder = "low_res/";
        }
    }

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


    public String grabTextFromImage(String image) throws Exception {
    	ImageRecognitionSettingsDTO settings = new ImageRecognitionSettingsDTO();
    	settings.setCrop(true);
    	ImageSearchDTO imageSearch = findImageOnScreen(image, settings);
        String text = ImageRecognition.getTextStringFromImage(imageSearch.getScreenshotFile());
		return text;
    }


}
