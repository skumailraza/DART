package com.dart.cameralibrary;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dart.paracamera.Camera;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Fragment responsible for capturing an image, processing it using OpenCV,
 * sending it to a server for OCR, and displaying the result.
 * It includes functionality for image skew correction and network communication.
 */
public class CameraFragment extends Fragment {
    // UI Elements
    private ImageView picFrame; // ImageView to display the captured and processed image.
    private TextView textView;  // TextView to display the OCR output received from the server.
    private RadioGroup language; // RadioGroup for language selection (though not directly used in this fragment's layout interaction).

    /**
     * Flag to indicate the selected language for OCR.
     * True for English, False for Urdu.
     * This flag is set by {@link CameraActivity} and used by {@link ClientThread}.
     */
    public static Boolean lFlag = false;

    private Camera camera; // Instance of the ParaCamera library for handling camera operations.
    private Socket socket; // Socket for network communication with the OCR server.

    // Flags to manage network connection status.
    public boolean clientConnected = false;
    public boolean serverConnected = false; // Note: ServerThread seems to act as a client, so this flag's usage might be misleading.

    private String ocrOutput = ""; // Stores the OCR result received from the server.

    // Network Configuration
    /** Port number for the OCR server. */
    private static final int SERVERPORT = 10000;
    /** Default IP address of the OCR server. Can be changed via {@link #setServerIp(String)}. */
    public static String SERVER_IP = "111.68.101.28";

    // Image data
    private Bitmap bitmap; // Holds the captured image.
    byte[] array;          // Byte array representation of the processed image, ready for network transmission.

    /**
     * Sets the server IP address.
     * @param ip The IP address string for the OCR server.
     */
    public void setServerIp(String ip){
        SERVER_IP = ip;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation). This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.result, container, false);
        // Initialize UI elements
        picFrame = (ImageView) rootView.findViewById(R.id.picFrame);
        textView = (TextView) rootView.findViewById(R.id.ocrOutput);
        language = (RadioGroup) rootView.findViewById(R.id.LangGroup); // This view is present in R.layout.result
        return rootView;
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize the ParaCamera library
        camera = new Camera.Builder()
                .resetToCorrectOrientation(true) // Ensure the image is rotated correctly based on EXIF data.
                .setTakePhotoRequestCode(1)      // Request code for onActivityResult.
                .setDirectory("pics")            // Subdirectory to save captured images.
                .setName("ali_" + System.currentTimeMillis()) // Unique name for each image.
                .setImageFormat(Camera.IMAGE_JPEG) // Desired image format.
                .setCompression(75)              // JPEG compression quality.
                .setImageHeight(1000)            // Target height for the image, maintaining aspect ratio.
                .build(this);                    // Build the camera instance for this fragment.
        try {
            // Open the camera and capture a picture.
            camera.takePicture();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error opening camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}. This follows the
     * related Activity API as described there in
     * {@link Fragment#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     * startActivityForResult(), allowing you to identify who this
     * result came from.
     * @param resultCode The integer result code returned by the child activity
     * through its setResult().
     * @param data An Intent, which can return result data to the caller
     * (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Camera.REQUEST_TAKE_PHOTO) { // Check if this is the result from ParaCamera
            Bitmap currentBitmap = camera.getCameraBitmap(); // Get the captured bitmap from ParaCamera.

            Log.i(TAG, "BITMAP CREATED!");
            if (currentBitmap != null) {
                this.bitmap = currentBitmap; // Store the bitmap.
                Mat mrgba = new Mat(); // OpenCV Mat object for image processing.

                // Convert Bitmap to Mat for OpenCV processing.
                Utils.bitmapToMat(this.bitmap, mrgba);

                // --- OpenCV Image Preprocessing Steps ---
                // 1. Convert to Grayscale: Simplifies image, reduces noise, necessary for many thresholding algorithms.
                Imgproc.cvtColor(mrgba, mrgba, Imgproc.COLOR_RGB2GRAY, 3);
                // 2. Adaptive Thresholding: Converts grayscale image to binary (black & white).
                //    ADAPTIVE_THRESH_MEAN_C: Threshold value is the mean of the neighbourhood area.
                //    THRESH_BINARY: Pixels > threshold become white (255), else black (0).
                //    15: Block size (size of the pixel neighborhood).
                //    9: Constant subtracted from the mean.
                Imgproc.adaptiveThreshold(mrgba, mrgba, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 9);
                // 3. Median Blur: Reduces salt-and-pepper noise.
                //    5: Kernel size (must be odd).
                Imgproc.medianBlur(mrgba,mrgba,5);

                Log.i(TAG, "Bitmap Thresholded");
                Toast.makeText(getActivity().getApplicationContext(), "Bitmap Thresholded!", Toast.LENGTH_SHORT).show();

                // Convert the processed Mat back to Bitmap to display.
                Utils.matToBitmap(mrgba, this.bitmap);
                picFrame.setImageBitmap(this.bitmap); // Display the processed image.

                // Convert the processed bitmap to a byte array for network transmission.
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                this.bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos); // PNG is lossless, compression value is ignored.
                array = bos.toByteArray();

                // If not already connected, start the client thread to send the image to the server.
                if (!clientConnected) {
                    if (!SERVER_IP.equals("")) {
                        Thread cThread = new Thread(new ClientThread());
                        cThread.start();
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), "Server IP not set!", Toast.LENGTH_LONG).show();
                    }
                    // These toasts might be premature as connection isn't confirmed yet.
                    Toast.makeText(getActivity().getApplicationContext(), "Server Connected!", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getActivity().getApplicationContext(), "Sending for OCR...", Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(getActivity().getApplicationContext(), "Waiting for Result...", Toast.LENGTH_LONG).show();

            }
            else {
                Toast.makeText(getActivity().getApplicationContext(), "Picture not taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Displays the OCR result received from the server in the TextView.
     */
    public void showOCRResult(){
            textView.setText(ocrOutput);
            textView.setVisibility(View.VISIBLE);
    }

    /**
     * Called when the fragment is no longer in use. This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     * Responsible for cleaning up resources.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up ParaCamera image file.
        if (camera != null) {
            camera.deleteImage();
        }
        // Recycle the bitmap to free memory.
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        // Consider closing the socket if it's still open.
        // However, socket management is primarily within ClientThread/ServerThread.
    }

    /**
     * Rotates the source matrix (image) by the specified angle around its center.
     * This is a common image preprocessing step to correct skew.
     *
     * @param src The source {@link Mat} (image) to be rotated.
     * @param angle The rotation angle in degrees. Positive values mean counter-clockwise rotation.
     */
    public void deskew(Mat src, double angle) {
        Point center = new Point(src.width() / 2, src.height() / 2); // Define the center of rotation.
        Mat rotImage = Imgproc.getRotationMatrix2D(center, angle, 1.0); // Get the 2x3 rotation matrix. 1.0 is scale.
        Size size = new Size(src.width(), src.height()); // Define the size of the output image (same as input).
        // Apply the affine transformation (rotation).
        // INTER_LINEAR: Bilinear interpolation for better quality.
        // CV_WARP_FILL_OUTLIERS: Fills outlier pixels (not used in this version of OpenCV constant).
        Imgproc.warpAffine(src, src, rotImage, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);
    }

    /**
     * Attempts to correct the skew of an image.
     * This method involves several steps: thresholding, morphological operations (erosion),
     * finding contours of white pixels, determining the minimum area rectangle enclosing these pixels,
     * and then using the angle of this rectangle to deskew the image.
     *
     * @param img The input {@link Mat} (image) to be skew-corrected.
     */
    public void correctSkew(Mat img) {
        Mat temp = img.clone(); // Work on a copy to avoid modifying the original image prematurely.
        // Binarize the image: pixels with value > 200 become 255 (white), others 0 (black).
        Imgproc.threshold(temp, temp, 200, 255, THRESH_BINARY);

        // Invert the colors: objects become white, background black. This is often needed for contour detection.
        Core.bitwise_not(temp, temp);
        // Define a structuring element for morphological operations.
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

        // Erode the image: shrinks white regions, helps to remove small noise or disconnect weakly connected components.
        Imgproc.erode(temp, temp, element);

        // Find all non-zero (white) pixels.
        Mat wLocMat = Mat.zeros(img.size(), temp.type());
        Core.findNonZero(temp, wLocMat);

        // Convert the locations of non-zero pixels to MatOfPoint.
        MatOfPoint matOfPoint = new MatOfPoint(wLocMat);

        // Convert MatOfPoint to MatOfPoint2f for minAreaRect.
        org.opencv.core.MatOfPoint2f mat2f = new org.opencv.core.MatOfPoint2f();
        matOfPoint.convertTo(mat2f, CvType.CV_32FC2);

        // Find the minimum area rotated rectangle that encloses all white pixels.
        RotatedRect rotatedRect = Imgproc.minAreaRect(mat2f);

        // Draw the contour of the found rectangle (optional, for visualization).
        Point[] vertices = new Point[4];
        rotatedRect.points(vertices);
        List<MatOfPoint> boxContours = new ArrayList<>();
        boxContours.add(new MatOfPoint(vertices));
        Imgproc.drawContours(temp, boxContours, 0, new Scalar(128, 128, 128), -1);

        // Adjust angle if width is greater than height (common for text lines).
        if (rotatedRect.size.width > rotatedRect.size.height) {
            rotatedRect.angle += 90.f;
        }
        Log.i(TAG, "ROTATION ANGLE: " + rotatedRect.angle);

        // Apply the deskew operation using the calculated angle.
        deskew(img, rotatedRect.angle);
    }

    /**
     * Detects and draws line segments in an image using OpenCV's Line Segment Detector (LSD).
     *
     * @param image The input {@link Mat} on which to detect lines. The input image is modified by Canny edge detection.
     * @return A new {@link Mat} with the detected line segments drawn on it.
     */
    public Mat lineSegment(Mat image) {
        Mat lineImage = image.clone(); // Draw lines on a copy of the original image.
        try {
            // Apply Canny edge detection first, as LSD often works best on edges.
            Imgproc.Canny(image, image, 50, 200); // Modifies the input 'image' Mat.
            // Create the Line Segment Detector.
            org.opencv.imgproc.LineSegmentDetector ls = Imgproc.createLineSegmentDetector();
            org.opencv.core.MatOfFloat4 lines = new org.opencv.core.MatOfFloat4(); // To store detected lines [x1, y1, x2, y2].
            // Detect lines.
            ls.detect(image, lines);
            // Draw the detected lines on 'lineImage'.
            ls.drawSegments(lineImage, lines);
        } catch (CvException e) {
            e.printStackTrace();
        }
        return lineImage;
    }

    /**
     * Rotates an image by a given angle (theta).
     * Special handling for angles around 90 degrees (uses transpose for potentially better quality/speed).
     *
     * @param rotImg The {@link Mat} to be rotated.
     * @param theta The angle in degrees for rotation.
     * @return The rotated {@link Mat}.
     */
    private static Mat RotateImage(Mat rotImg, double theta) {
        Mat rotatedImage = new Mat();
        // Specific handling for angles near 90 degrees: transpose the image.
        // This might be an optimization or a way to handle specific orientations.
        if (theta >= 92 && theta <= 93) {
            Core.transpose(rotImg, rotatedImage);
        } else {
            Point center = new Point(rotImg.cols() / 2.0, rotImg.rows() / 2.0); // Center of rotation.
            Mat rotMatrix = Imgproc.getRotationMatrix2D(center, theta, 1.0); // Get rotation matrix.
            Imgproc.warpAffine(rotImg, rotatedImage, rotMatrix, rotImg.size()); // Apply rotation.
        }
        return rotatedImage;
    }

    /**
     * Detects the skew angle of an image, primarily for text images.
     * It works by rotating the image by small angles, calculating horizontal projections
     * of white pixels for each rotation, and finding the angle that maximizes a criterion function
     * based on these projections (likely looking for strongest horizontal lines).
     *
     * @param mat The input {@link Mat} (expected to be a binary image where text is white).
     * @return The detected skew angle in degrees.
     */
    private static int skewDetectImageRotation(Mat mat) {
        int[] projections; // Array to store horizontal projection profile.
        HashMap<Integer, Double> angle_measure = new HashMap<>(); // Map to store angle and its corresponding criterion value.

        // Iterate through a range of small angles.
        for (int theta = -15; theta <= 15; theta++) {
            if (theta == 0 || theta == -1 || theta == 1) // Skip minor angles or zero.
                continue;
            Mat rotImage = RotateImage(mat.clone(), -theta); // Rotate image by -theta. Use clone to preserve original 'mat'.
            projections = new int[rotImage.rows()]; // Initialize projection array for current rotated image.
            // Calculate horizontal projection: sum of white pixels per row.
            for (int i = 0; i < rotImage.rows(); i++) {
                for (int j = 0; j < rotImage.cols(); j++) {
                    double[] pixVal = rotImage.get(i, j);
                    if (pixVal != null && pixVal[0] == 255) { // Check for white pixel.
                        projections[i]++;
                    }
                }
            }
            angle_measure.put(theta, criterion_func(projections)); // Store angle and its criterion value.
        }

        int angle = 0; // Best angle found.
        double val = Double.NEGATIVE_INFINITY; // Max criterion value found.
        // Find the angle that yielded the maximum criterion value.
        for (HashMap.Entry<Integer, Double> entry : angle_measure.entrySet()) {
            if (val < entry.getValue()) {
                val = entry.getValue();
                angle = entry.getKey();
            }
        }
        return angle;
    }

    /**
     * Criterion function used in skew detection.
     * Calculates a measure from the horizontal projection profile.
     * In this case, it simply returns the maximum value in the projection array.
     * A higher maximum suggests stronger horizontal alignment of pixels (e.g., text lines).
     *
     * @param projections An array representing the horizontal projection profile (sum of white pixels per row).
     * @return The maximum value in the projections array.
     */
    private static double criterion_func(int[] projections) {
        double max = 0;
        for (int projection : projections) {
            if (max < projection) {
                max = projection;
            }
        }
        return max;
    }

    /**
     * Calculates the new row position of a point (x1, y1) after rotating it by theta degrees
     * around the center (x0, y0) of the given matrix 'mat'.
     * This seems to be a helper function, possibly for transforming coordinates during rotation,
     * though it's not directly used by other skew correction methods in this file.
     *
     * @param y1 Original y-coordinate of the point.
     * @param x1 Original x-coordinate of the point.
     * @param theta Rotation angle in degrees.
     * @param mat The {@link Mat} defining the coordinate system and center of rotation.
     * @return The new row (y-coordinate) after rotation.
     */
    private static int rotate(double y1, double x1, int theta, Mat mat) {
        double x0 = mat.cols() / 2.0; // Center x-coordinate.
        double y0 = mat.rows() / 2.0; // Center y-coordinate.
        double thetaRad = Math.toRadians(theta); // Convert angle to radians for Math functions.

        // Standard 2D rotation formulas.
        // int new_col = (int) ((x1 - x0) * Math.cos(thetaRad) - (y1 - y0) * Math.sin(thetaRad) + x0);
        int new_row = (int) ((x1 - x0) * Math.sin(thetaRad) + (y1 - y0) * Math.cos(thetaRad) + y0);

        return new_row;
    }

    /**
     * Runnable task for handling client-side network communication.
     * Connects to the OCR server, sends the processed image data,
     * and receives the OCR result. Updates the UI with the result.
     */
    public class ClientThread implements Runnable {
        private BufferedReader input; // For reading data from the server.
        Handler handler = new Handler(); // Handler to post UI updates from this background thread.

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP); // Resolve server IP.
                Log.d("ClientActivity", "C: Connecting...");
                socket = new Socket(serverAddr, SERVERPORT); // Establish socket connection.
                clientConnected = true; // Set connection flag.
                StringBuilder stringBuilder = new StringBuilder(); // To accumulate server response.

                // --- Send data to server ---
                try {
                    Log.d("ClientActivity", "C: Sending command.");
                    OutputStream output = socket.getOutputStream(); // Get output stream.
                    Log.d("ClientActivity", "C: image writing.");

                    // Prepend language flag if English is selected.
                    // The server likely uses this to choose the OCR language model.
                    if (lFlag) { // lFlag is true for English
                        output.write("ENGLISH\n".getBytes());
                    }
                    output.write(array); // Write image byte array.
                    output.flush();      // Ensure data is sent.
                    Log.d("ClientActivity", "C: Sent.");
                }
                catch(Exception e){
                        e.printStackTrace();
                        // Consider closing socket or setting clientConnected = false here.
                }

                // --- Receive data from server ---
                try{
                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    // Read lines until end of stream.
                    while ((line = input.readLine()) != null) {
                        stringBuilder.append(line).append('\n');
                    }
                    Log.d("Server Reply: ", "Server Reply Received!");
                    ocrOutput = stringBuilder.toString();
                    // Remove trailing newline if server sends one, for cleaner display.
                    if (ocrOutput.endsWith("\n")) {
                        ocrOutput = ocrOutput.substring(0, ocrOutput.length() -1);
                    }

                    // Update UI with the received OCR text.
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(ocrOutput);
                        }
                    });

                } catch (Exception e) {
                    Log.e("ClientActivity", "S: Error receiving/processing server reply", e);
                }

                // Close socket after communication.
                socket.close();
                Log.d("ClientActivity", "C: Closed.");
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error in client thread", e);
                clientConnected = false; // Reset connection flag on error.
            }
        }
    }

    /**
     * Runnable task that seems intended for server-side logic or a persistent client connection
     * to receive updates. However, its current implementation connects to `SERVER_IP` and `SERVERPORT`
     * much like `ClientThread`, suggesting it might be another client implementation or an
     * incompletely refactored piece of code.
     *
     * It attempts to connect and then continuously read data from the socket.
     * The `serverConnected` flag is used to control its loop.
     */
    public class ServerThread implements Runnable {
        private BufferedReader input; // For reading data from the socket.
        Handler handler = new Handler(); // Handler for UI updates.

        @Override
        public void run() {
            synchronized (this) { // Synchronized block, though not clear what resource it's protecting here.
                Socket threadSocket = null; // Local socket instance for this thread.
                try {
                    // Attempt to connect to the defined SERVER_IP and SERVERPORT.
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                    threadSocket = new Socket(serverAddr, SERVERPORT);
                    serverConnected = true; // Mark as connected.
                } catch (Exception e) {
                    e.printStackTrace();
                    serverConnected = false; // Ensure flag is false on connection failure.
                    return; // Exit if connection fails.
                }

                // Loop while not interrupted, connected, and socket is valid.
                while (!Thread.currentThread().isInterrupted() && serverConnected && threadSocket != null && threadSocket.isConnected()) {
                    try {
                        this.input = new BufferedReader(new InputStreamReader(threadSocket.getInputStream()));
                        String line;
                        StringBuilder stringBuilder = new StringBuilder();

                        // Read lines from the socket. This will block until data is received or connection is closed.
                        while ((line = input.readLine()) != null) {
                            stringBuilder.append(line).append('\n');
                        }
                        ocrOutput = stringBuilder.toString();
                        // Clean trailing newline.
                         if (ocrOutput.endsWith("\n")) {
                            ocrOutput = ocrOutput.substring(0, ocrOutput.length() -1);
                        }

                        // Update UI with received text.
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(ocrOutput);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        serverConnected = false; // Stop loop on IO error (e.g. server disconnects).
                    } catch (Exception e) { // Catch other potential errors.
                        e.printStackTrace();
                        serverConnected = false; // Stop loop on other errors.
                    }
                }

                // Clean up: close the socket.
                try {
                    if (threadSocket != null) {
                        threadSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Ensure serverConnected is false when thread exits its main loop or socket is closed.
                serverConnected = false;
            }
        }
    }
}

