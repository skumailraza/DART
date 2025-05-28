# DART - Deep Address Recognition Technology

## Overview

DART (Deep Address Recognition Technology) is an Android application designed to recognize handwritten addresses on postal envelopes. It utilizes a custom Deep Neural Network (CRNN) architecture, trained on the NUST-UHWR Dataset for Urdu and the Forms Dataset for English, enabling it to process addresses in both languages. The application captures images via the device camera, preprocesses them using OpenCV, and then communicates with a high-performance GPU server for the actual address recognition.

## Key Features

*   Recognizes both English and Urdu handwritten addresses.
*   Integrates with a custom camera solution (ParaCamera) for image capture.
*   Performs on-device image preprocessing using OpenCV.
*   Communicates with a remote server for OCR processing.

## ParaCamera Library Usage

The DART application uses the ParaCamera library to handle camera operations.

### 1. Initialize Camera

First, create a global camera reference in your Activity or Fragment:

```java
Camera camera;
```

Then, build the camera instance with your desired configurations:

```java
// Build the camera
camera = new Camera.Builder()
                .resetToCorrectOrientation(true) // Rotates the camera bitmap to the correct orientation based on metadata
                .setTakePhotoRequestCode(1)      // Custom request code for onActivityResult
                .setDirectory("pics")            // Directory to save the image
                .setName("ali_" + System.currentTimeMillis()) // Image file name
                .setImageFormat(Camera.IMAGE_JPEG) // Image format (JPG, PNG)
                .setCompression(75)              // Image compression quality (0-100)
                .setImageHeight(1000)            // Target image height, maintaining aspect ratio
                .build(this);                    // Context (Activity or Fragment)
```

### 2. Capture Image

Call the `takePicture()` method to open the camera interface:

```java
try {
    camera.takePicture();
} catch (Exception e) {
    e.printStackTrace();
}
```
You will receive the result in the `onActivityResult` method of your Activity or Fragment.

## Image Preprocessing with OpenCV

After capturing the image, it's preprocessed using OpenCV before being sent to the server:

```java
Bitmap bitmap = camera.getCameraBitmap(); // Assuming 'camera' is your initialized ParaCamera object
Mat mrgba = new Mat();

if (bitmap != null) {
    Utils.bitmapToMat(bitmap, mrgba);
    Imgproc.cvtColor(mrgba, mrgba, Imgproc.COLOR_RGB2GRAY, 3);
    Imgproc.adaptiveThreshold(mrgba, mrgba, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 9);
    Imgproc.medianBlur(mrgba, mrgba, 5);

    // Convert the processed Mat back to Bitmap if needed for display or further local processing
    Utils.matToBitmap(mrgba, bitmap);

    // Prepare byte array for server transmission
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
    byte[] array = bos.toByteArray();
    // 'array' is now ready to be sent to the server
}
```

## Server Communication (Conceptual)

### Sending Image Data

The preprocessed image (as a byte array) is sent to the DART recognition server:

```java
// Conceptual: 'socket' and 'array' (from preprocessing) must be defined
// OutputStream output = socket.getOutputStream();
// output.write(array);
// output.flush();
// Log.d("ClientActivity", "C: Sent.");
```

### Receiving Recognized Address

The server's response, containing the recognized address, is then received and displayed:

```java
// Conceptual: 'socket', 'handler', and 'textView' must be defined
// BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
// StringBuilder stringBuilder = new StringBuilder();
// String line;
// while ((line = input.readLine()) != null) {
//     stringBuilder.append(line).append('\n');
// }
// final String ocrOutput = stringBuilder.toString().trim();

// handler.post(new Runnable() {
//     @Override
//     public void run() {
//         textView.setText(ocrOutput);
//     }
// });
```

## Required Android Permissions

To function correctly, the DART application requires the following permissions in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
```
*Note: `READ_NETWORK_STATE` was listed in the original README, but `INTERNET` implies network access. `CAMERA` permission is essential and was added.*

## OpenCV Manager

The DART application relies on OpenCV (Open Source Computer Vision Library). For Android, OpenCV functionality is often provided via the OpenCV Manager application.

### Purpose

OpenCV Manager provides the necessary OpenCV binaries to applications that use the library. This approach helps reduce the overall size of applications by not bundling the OpenCV binaries directly within each app. It also allows for system-wide updates of the OpenCV library.

### Manual Installation

If Google Play is not available on the device (e.g., emulators, development boards), or if a specific version is required, OpenCV Manager can be installed manually using the Android Debug Bridge (adb):

```bash
adb install <path-to-OpenCV-sdk>/apk/OpenCV_3.1.0_Manager_3.10_<platform>.apk
```

Replace `<path-to-OpenCV-sdk>` with the actual path to your OpenCV SDK and `<platform>` with the target architecture.

### OpenCV Manager APKs

Choose the appropriate OpenCV Manager APK for your device's platform:

*   `OpenCV_3.1.0_Manager_3.10_armeabi.apk` - armeabi (ARMv5, ARMv6)
*   `OpenCV_3.1.0_Manager_3.10_armeabi-v7a.apk` - armeabi-v7a (ARMv7-A + NEON)
*   `OpenCV_3.1.0_Manager_3.10_arm64-v8a.apk` - arm64-v8a (ARM64-v8a)
*   `OpenCV_3.1.0_Manager_3.10_mips.apk` - mips (MIPS)
*   `OpenCV_3.1.0_Manager_3.10_mips64.apk` - mips64 (MIPS64)
*   `OpenCV_3.1.0_Manager_3.10_x86.apk` - x86
*   `OpenCV_3.1.0_Manager_3.10_x86_64.apk` - x86_64

*(Note: These APK versions (3.1.0 for library, 3.10 for Manager) are based on the provided text and might be outdated. Always use versions compatible with your project's OpenCV SDK.)*

### OpenCV Android Documentation

For more detailed information about OpenCV on Android, refer to the official documentation:
[http://opencv.org/platforms/android.html](http://opencv.org/platforms/android.html)

## Build and Run

Instructions on how to build and run this project will be added here.
