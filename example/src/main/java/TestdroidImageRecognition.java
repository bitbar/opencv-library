import imagerecognition.ImageRecognition;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import objects.ImageLocation;
import objects.ImageRecognitionSettings;
import objects.ImageSearchResult;


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

    public ImageLocation findImageOnScreen(String image) throws Exception {
        ImageRecognitionSettings defaultSettings = new ImageRecognitionSettings();
        return findImageOnScreen(image, defaultSettings).getImageLocation();
    }

    public ImageSearchResult findImageOnScreen(String imageName, ImageRecognitionSettings settings) throws Exception { 
        // queryImageFolder is "", unless set by setQueryImageFolder()
        String queryImageFolder = "queryimages/" + queryImageSubFolder;
        String screenshotsFolder = "target/reports/screenshots/";
        String imageFile = queryImageFolder+imageName;
        log("Searching for: "+imageFile);
        ImageSearchResult foundImage = ImageRecognition.findImageOnScreen(imageFile, screenshotsFolder, settings, platform);
        return foundImage;
    }

    public void waitForImageToDisappearFromScreen(String image) throws Exception {
        String queryImageFolder = "queryimages/" + queryImageSubFolder;
        String screenshotsFolder = "target/reports/screenshots/";
        String imageFile = queryImageFolder+image;
        boolean hasImageDisappeared = ImageRecognition.hasImageDissappearedFromScreenBeforeTimeout(imageFile, screenshotsFolder, platform);
        assert(hasImageDisappeared);
    }


    public String grabTextFromImage(String image) throws Exception {
        ImageSearchResult imageSearch = findAndCropImage(image);
        String text = ImageRecognition.getTextStringFromImage(imageSearch.getScreenshotFile());
        return text;
    }

    public ImageSearchResult findAndCropImage(String image) throws Exception {
        ImageRecognitionSettings settings = new ImageRecognitionSettings();
        settings.setCrop(true);
        ImageSearchResult imageSearch = findImageOnScreen(image, settings);
        return imageSearch;
    }


}
