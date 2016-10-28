package dtos;

import org.opencv.core.Point;

public class ImageSearchDTO {
	
	public ImageSearchDTO(){
		this.screenshotFile = null;
		this.imageRectangle = null;
	}
	
	public ImageSearchDTO(String screenshotFile, Point[] imageRectangle){
		this.screenshotFile = screenshotFile;
		this.imageRectangle = imageRectangle;
	}
	
	public boolean isFound(){
		return screenshotFile!=null && imageRectangle!=null;
	}
	
	public String getScreenshotFile() {
		return screenshotFile;
	}
	public void setScreenshotFile(String screenshotFile) {
		this.screenshotFile = screenshotFile;
	}

	public Point[] getImageRectangle() {
		return imageRectangle;
	}

	public void setImageRectangle(Point[] imageRectangle) {
		this.imageRectangle = imageRectangle;
	}

	private String screenshotFile;
	private Point[] imageRectangle;
}
