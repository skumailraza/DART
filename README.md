# DART
DEEP ADDRESS RECOGNITION TECHNOLOGY


[ ![Download](https://api.bintray.com/packages/janishar/mindorks/paracamera/images/download.svg) ](https://bintray.com/janishar/mindorks/paracamera/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-ParaCamera-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4415)

##Android Camera OCR App to recognize addresses on Postal Envelopes.

##1. Capture Camera Image
```java
// Create global camera reference in an activity or fragment
Camera camera;

// Build the camera   
camera = new Camera.Builder()
                .resetToCorrectOrientation(true)// it will rotate the camera bitmap to the correct orientation from meta data
                .setTakePhotoRequestCode(1)
                .setDirectory("pics")
                .setName("ali_" + System.currentTimeMillis())
                .setImageFormat(Camera.IMAGE_JPEG)
                .setCompression(75)
                .setImageHeight(1000)// it will try to achieve this height as close as possible maintaining the aspect ratio; 
                .build(this);
```

```java
// Call the camera takePicture method to open the existing camera             
        try {
            camera.takePicture();
        }catch (Exception e){
            e.printStackTrace();
        }
```
##2. Preprocess the image using OpenCV
```java
                Utils.bitmapToMat(bitmap, mrgba);
                Imgproc.cvtColor(mrgba, mrgba, Imgproc.COLOR_RGB2GRAY, 3);
                Imgproc.adaptiveThreshold(mrgba, mrgba, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 9);
                Imgproc.medianBlur(mrgba,mrgba,5);

```
```java
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                array = bos.toByteArray();
```

##3. Send the image to the Recognition Server
```java
                while (clientConnected) {
                    try {

                        Log.d("ClientActivity", "C: Sending command.");

                        OutputStream output = socket.getOutputStream();
                        Log.d("ClientActivity", "C: image writing.");
                        output.write(array);
                        output.flush();
                        // out.println("Hey Server!");
                        Log.d("ClientActivity", "C: Sent.");

                    } catch (Exception e) {
                        Log.e("ClientActivity", "S: Error", e);
                }
            }
```
##4. Receive the Recognized Address and Display
```java
                        socket = serverSocket.accept();

                        Log.i("Server Thread:", "Started!");
                        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String line = null;
                        while ((line = input.readLine()) != null) {
                            stringBuilder.append(line + '\n');
                        }
                        ocrOutput = stringBuilder.toString();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(ocrOutput);
                            }
                        });
```
### WRITE_EXTERNAL_STORAGE is required
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
### INTERNET PERMISSION is required
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
### NETWORK STATE PERMISSION is required
```xml
<uses-permission android:name="android.permission.READ_NETWORK_STATE" />
```
