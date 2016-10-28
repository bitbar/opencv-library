package library;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.opencv.core.Point;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dtos.ImageRecognitionSettingsDTO;

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
                imgRect = scaleImageRectangleForIos(screenSize, imgRect);
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



	private static Point[] scaleImageRectangleForIos(Dimension screenSize, Point[] imgRect) {
		Point[] imgRectScaled;
		//for retina devices we need to recalculate coordinates
		double sceneHeight = imageFinder.getSceneHeight();
		double sceneWidth = imageFinder.getSceneWidth();

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
    
    
    
}
