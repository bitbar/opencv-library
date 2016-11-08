import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

public class AppiumCommons {
    private static Logger logger = LoggerFactory.getLogger(AppiumCommons.class);

    public static void hideKeyboard(AppiumDriver<MobileElement> driver) {
        try {
            driver.hideKeyboard();
        } catch (Exception e) {
            logger.debug("Hiding soft keyboard failed");
            logger.debug(e.toString());
        }
    }

    public static void takeAppiumScreenshot(AppiumDriver<MobileElement> driver, String fullFileName) {
        File scrFile = driver.getScreenshotAs(OutputType.FILE);
        try {
            File testScreenshot = new File(fullFileName);
            FileUtils.copyFile(scrFile, testScreenshot);
            logger.info("Screenshot stored to {}", testScreenshot.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void swipeUp(AppiumDriver<MobileElement> driver) {
        swipeUp(driver, 0.15f, 0.15f);
    }

    public static void swipeUp(AppiumDriver<MobileElement> driver, float topPad, float bottomPad) {
        Dimension size = driver.manage().window().getSize();
        logger.debug("Window size is " + size);
        swipeUp(driver, new Point(0, 0), size, 1000, topPad, bottomPad);
    }

    public static void swipeUp(AppiumDriver<MobileElement> driver, Point rootLocation, Dimension rootSize, int duration, float topPad, float bottomPad) {
        int offset = 1;
        int topOffset = Math.round(rootSize.getHeight() * topPad);
        int bottomOffset = Math.round(rootSize.getHeight() * bottomPad);
        Point center = new Point(rootLocation.x + rootSize.getWidth() / 2, rootLocation.y + rootSize.getHeight() / 2);
        logger.debug("Swiping up at" +
                " x1: " + center.getX() +
                " y1:" + (rootLocation.getY() + rootSize.getHeight() - bottomOffset + offset) +
                " x2:" + center.getX() +
                " y2:" + (rootLocation.getY() + topOffset));
        driver.swipe(center.getX(),
                rootLocation.getY() + rootSize.getHeight() - bottomOffset + offset,
                center.getX(),
                rootLocation.getY() + topOffset,
                duration);
    }

    public static void swipeDown(AppiumDriver<MobileElement> driver) {
        swipeDown(driver, 0.15f, 0.15f);
    }

    public static void swipeDown(AppiumDriver<MobileElement> driver, float topPad, float bottomPad) {
        Dimension size = driver.manage().window().getSize();
        logger.debug("Window size is " + size);
        swipeDown(driver, new Point(0, 0), size, 1000, topPad, bottomPad);
    }

    public static void swipeDown(AppiumDriver<MobileElement> driver, Point rootLocation, Dimension rootSize, int duration, float topPad, float bottomPad) {
        int offset = 1;
        int topOffset = Math.round(rootSize.getHeight() * topPad);
        int bottomOffset = Math.round(rootSize.getHeight() * bottomPad);
        Point center = new Point(rootLocation.x + rootSize.getWidth() / 2, rootLocation.y + rootSize.getHeight() / 2);
        logger.debug("Swiping down at" +
                " x1: " + center.getX() +
                " y1:" + (rootLocation.getY() + topOffset) +
                " x2:" + center.getX() +
                " y2:" + (rootLocation.getY() + rootSize.getHeight() - bottomOffset + offset));
        driver.swipe(center.getX(),
                (rootLocation.getY() + topOffset),
                center.getX(),
                (rootLocation.getY() + rootSize.getHeight() - bottomOffset + offset),
                duration);
    }

    public static void swipeLeft(AppiumDriver<MobileElement> driver) {
        swipeLeft(driver, 0.15f, 0.15f);
    }

    public static void swipeLeft(AppiumDriver<MobileElement> driver, float leftPad, float rightPad) {
        Dimension size = driver.manage().window().getSize();
        logger.debug("Window size " + size);
        swipeLeft(driver, new Point(0,0), size, 1000, leftPad, rightPad);
    }

    public static void swipeLeft(AppiumDriver<MobileElement> driver, Point rootLocation, Dimension rootSize, int duration, float leftPad, float rightPad) {
        int offset = 1;
        int leftOffset = Math.round(rootSize.getWidth() * leftPad);
        int rightOffset = Math.round(rootSize.getWidth() * rightPad);
        Point center = new Point(rootLocation.x + rootSize.getWidth() / 2, rootLocation.y + rootSize.getHeight() / 2);
        logger.debug("Swiping left at" +
                " x1: " + (rootLocation.getX() + rootSize.getWidth() - rightOffset + offset) +
                " y1:" + center.getY() +
                " x2:" + (rootLocation.getX() + leftOffset) +
                " y2:" + center.getY());
        driver.swipe((rootLocation.getX() + rootSize.getWidth() - rightOffset + offset),
                center.getY(),
                (rootLocation.getX() + leftOffset),
                center.getY(),
                duration);
    }

    public static void swipeRight(AppiumDriver<MobileElement> driver) {
        swipeRight(driver, 0.15f, 0.15f);
    }

    public static void swipeRight(AppiumDriver<MobileElement> driver, float leftPad, float rightPad) {
        Dimension size = driver.manage().window().getSize();
        swipeRight(driver, new Point(0,0), size, 1000, leftPad, rightPad);
    }

    public static void swipeRight(AppiumDriver<MobileElement> driver, Point rootLocation, Dimension rootSize, int duration, float leftPad, float rightPad) {
        int offset = 1;
        int leftOffset = Math.round(rootSize.getWidth() * leftPad);
        int rightOffset = Math.round(rootSize.getWidth() * rightPad);
        Point center = new Point(rootLocation.x + rootSize.getWidth() / 2, rootLocation.y + rootSize.getHeight() / 2);
        logger.debug("Swiping right at" +
                " x1: " + (rootLocation.getX() + leftOffset) +
                " y1:" + center.getY() +
                " x2:" + (rootLocation.getX() + rootSize.getWidth() - rightOffset + offset) +
                " y2:" + center.getY());
        driver.swipe((rootLocation.getX() + leftOffset),
                center.getY(),
                (rootLocation.getX() + rootSize.getWidth() - rightOffset + offset),
                center.getY(),
                duration);
    }

}
