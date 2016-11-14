# Prerequisites
## General

Appium

## Android
Android SDK

### Notes

```
export PATH=$PATH:$ANDROID_HOME/platform-tools/
export PATH=${PATH}:$ANDROID_HOME/tools/
```

## iOS

libimobiledevice

### Notes

```
brew uninstall libimobiledevice
brew install --HEAD libimobiledevice
```

# Getting started
Run Appium:

` appium` or `appium -U <udid>`


```
cd library
mvn package
cp target/image-recognition-library-2.0-SNAPSHOT.jar ../example/lib/image_recognition_library_test.jar
cd ../example
	
# Download the example Android application
wget https://github.com/bitbar/testdroid-samples/blob/master/apps/builds/BitbarSampleApp.apk -O application.apk
mvn -Dtest=AndroidSample clean test
	
# Download the example iOS application
wget https://github.com/bitbar/testdroid-samples/blob/master/apps/builds/BitbarIOSSample.ipa -O application.ipa
export UDID=<iPhone udid>
mvn -Dtest=iOSSample clean test
```

