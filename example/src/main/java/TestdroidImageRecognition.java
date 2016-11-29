import imagerecognition.ImageRecognition;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import objects.ImageLocation;
import objects.ImageRecognitionSettings;
import objects.ImageSearchResult;

public class TestdroidImageRecognition extends AbstractAppiumTest {

    public Logger logger = LoggerFactory.getLogger(TestdroidImageRecognition.class);
    String screenshotsFolder;
    String queryImageFolder;
    
    public TestdroidImageRecognition(){
        super();
        screenshotsFolder = "target/reports/screenshots/";
        queryImageFolder = "queryimages/";
        File dir = new File(screenshotsFolder);
        dir.mkdirs();
    }

    public ImageLocation findImageOnScreen(String image) throws Exception {
        ImageRecognitionSettings defaultSettings = new ImageRecognitionSettings();
        return findImageOnScreen(image, defaultSettings).getImageLocation();
    }

    public ImageSearchResult findImageOnScreen(String imageName, ImageRecognitionSettings settings) throws Exception {
        String imageFile = queryImageFolder+imageName;
        log("Searching for: "+imageFile);
        ImageSearchResult foundImage = ImageRecognition.findImageOnScreen(imageFile, screenshotsFolder, settings, platform);
        return foundImage;
    }

    public void waitForImageToDisappearFromScreen(String image) throws Exception {
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
