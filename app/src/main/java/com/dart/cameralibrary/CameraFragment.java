package com.dart.cameralibrary;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.LineSegmentDetector;

import static android.app.Activity.RESULT_CANCELED;
import static android.content.ContentValues.TAG;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;


import com.dart.paracamera.Camera;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Exchanger;


public class CameraFragment extends Fragment {
    private ImageView picFrame;
    private TextView textView;
    private Camera camera;
    private Socket socket;
    private ServerSocket serverSocket = null;
    public boolean clientConnected = false;
    public boolean serverConnected = false;
    private String ocrOutput = "";


    private static int SERVERPORT = 10000;
    private static String SERVER_IP = "192.168.1.6";

    private Mat mIntermediateMat;
    private Bitmap bitmap;
    byte[] array;

    public void setServerIp(String ip){
        SERVER_IP = ip;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.result, container, false);
        picFrame = (ImageView) rootView.findViewById(R.id.picFrame);
        textView = (TextView) rootView.findViewById(R.id.ocrOutput);
//        EditText ipAddr = (EditText)rootView.findViewById(R.id.ip_address);
//        setServerIp(ipAddr.getText().toString());
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        camera = new Camera.Builder()
                .resetToCorrectOrientation(true)
                .setTakePhotoRequestCode(1)
                .setDirectory("pics")
                .setName("ali_" + System.currentTimeMillis())
                .setImageFormat(Camera.IMAGE_JPEG)
                .setCompression(75)
                .setImageHeight(1000)
                .build(this);
        try {
            camera.takePicture();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        bitmap = null;
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
            Bitmap bitmap = camera.getCameraBitmap();

            Log.i(TAG, "BITMAP CREATED!");
            if (bitmap != null) {
                Mat mrgba = new Mat();
                Utils.bitmapToMat(bitmap, mrgba);
                Imgproc.cvtColor(mrgba, mrgba, Imgproc.COLOR_RGB2GRAY, 3);
                Imgproc.adaptiveThreshold(mrgba, mrgba, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 9);
                Imgproc.medianBlur(mrgba,mrgba,5);

                Log.i(TAG, "Bitmap Thresholded");
                Toast.makeText(getActivity().getApplicationContext(), "Bitmap Thresholded!", Toast.LENGTH_SHORT).show();

                Utils.matToBitmap(mrgba, bitmap);
                picFrame.setImageBitmap(bitmap);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                array = bos.toByteArray();

                synchronized (this) {
                    if (!serverConnected){
                        if (!SERVER_IP.equals("")) {
                            Thread sThread = new Thread(new ServerThread());
                            sThread.start();
                        }
                    }
                }
                if (!clientConnected) {
                    if (!SERVER_IP.equals("")) {
                        Thread cThread = new Thread(new ClientThread());
                        cThread.start();
                    }
                    Toast.makeText(getActivity().getApplicationContext(), "Server Connected!", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getActivity().getApplicationContext(), "Sending for OCR...", Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(getActivity().getApplicationContext(), "Waiting for Result...", Toast.LENGTH_LONG).show();

//                if (!serverConnected) {
//                    if (!SERVER_IP.equals("")) {
//                        Thread sThread = new Thread(new ServerThread());
//                        sThread.start();
//                    }
//                }
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Picture not taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showOCRResult(){
            textView.setText(ocrOutput);
            textView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.deleteImage();
        bitmap = null;
    }

    public void deskew(Mat src, double angle) {
        Point center = new Point(src.width() / 2, src.height() / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        //1.0 means 100 % scale
        Size size = new Size(src.width(), src.height());
        Imgproc.warpAffine(src, src, rotImage, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);
        //return src;
    }

    public void correctSkew(Mat img) {
        //Load this image in grayscale
        //Mat img = Imgcodecs.imread( inFile, Imgcodecs.IMREAD_GRAYSCALE );

        //Binarize it
        //Use adaptive threshold if necessary
        //Imgproc.adaptiveThreshold(img, img, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);
        Mat temp = img.clone();
        Imgproc.threshold(temp, temp, 200, 255, THRESH_BINARY);

        //Invert the colors (because objects are represented as white pixels, and the background is represented by black pixels)
        Core.bitwise_not(temp, temp);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

        //We can now perform our erosion, we must declare our rectangle-shaped structuring element and call the erode function
        Imgproc.erode(temp, temp, element);

        //Find all white pixels
        Mat wLocMat = Mat.zeros(img.size(), temp.type());
        Core.findNonZero(temp, wLocMat);

        //Create an empty Mat and pass it to the function
        MatOfPoint matOfPoint = new MatOfPoint(wLocMat);

        //Translate MatOfPoint to MatOfPoint2f in order to user at a next step
        MatOfPoint2f mat2f = new MatOfPoint2f();
        matOfPoint.convertTo(mat2f, CvType.CV_32FC2);

        //Get rotated rect of white pixels
        RotatedRect rotatedRect = Imgproc.minAreaRect(mat2f);

        Point[] vertices = new Point[4];
        rotatedRect.points(vertices);
        List<MatOfPoint> boxContours = new ArrayList<>();
        boxContours.add(new MatOfPoint(vertices));
        Imgproc.drawContours(temp, boxContours, 0, new Scalar(128, 128, 128), -1);


        if (rotatedRect.size.width > rotatedRect.size.height) {
            rotatedRect.angle += 90.f;
        }
        Log.i(TAG, "ROTATION ANGLE: " + rotatedRect.angle);
        double resultAngle = rotatedRect.angle;

        //Or
        //rotatedRect.angle = rotatedRect.angle < -45 ? rotatedRect.angle + 90.f : rotatedRect.angle;

        deskew(img, rotatedRect.angle);
        //Imgcodecs.imwrite( outputFile, result );
        //return result;

    }

    public Mat lineSegment(Mat image) {

        Mat lineImage = image;

        try {
            Imgproc.Canny(image, image, 50, 200);
            LineSegmentDetector ls = Imgproc.createLineSegmentDetector();

            double start = Core.getTickCount();

            MatOfFloat4 lines = new MatOfFloat4();

            ls.detect(image, lines);

            double duration_ms = (Core.getTickCount() - start) * 1000 / Core.getTickFrequency();

            ls.drawSegments(lineImage, lines);

        } catch (CvException e) {
            e.printStackTrace();
        }
        return lineImage;
    }

    private static Mat RotateImage(Mat rotImg, double theta) {
        double angleToRot = theta;

        Mat rotatedImage = new Mat();
        if (angleToRot >= 92 && angleToRot <= 93) {
            Core.transpose(rotImg, rotatedImage);
        } else {
            org.opencv.core.Point center = new org.opencv.core.Point(rotImg.cols() / 2, rotImg.rows() / 2);
            Mat rotImage = Imgproc.getRotationMatrix2D(center, angleToRot, 1.0);

            Imgproc.warpAffine(rotImg, rotatedImage, rotImage, rotImg.size());
        }

        return rotatedImage;

    }

    private static int skewDetectImageRotation(Mat mat) {
        int[] projections = null;
        HashMap<Integer, Double> angle_measure = new HashMap<Integer, Double>();

        for (int theta = -15; theta <= 15; theta = theta + 1) {
            if (theta == 0 || theta == -1 || theta == 1)
                continue;
            Mat rotImage = RotateImage(mat, -theta);
            projections = new int[mat.rows()];
            for (int i = 0; i < mat.rows(); i++) {
                double[] pixVal;
                for (int j = 0; j < mat.cols(); j++) {

                    pixVal = rotImage.get(i, j);
                    if (pixVal[0] == 255) {
                        projections[i]++;
                    }
                }
            }
            Mat tempMat = rotImage;

            angle_measure.put(theta, criterion_func(projections));
        }
        int angle = 0;
        double val = 0;
        for (int k : angle_measure.keySet()) {
            if (val < angle_measure.get(k)) {
                val = angle_measure.get(k);
                angle = k;
            }
        }
        return angle;
    }

    private static double criterion_func(int[] projections) {
        double max = 0;

        for (int i = 0; i < projections.length; i++) {
            if (max < projections[i]) {
                max = projections[i];
            }
        }

        return max;
    }

//    private static void DrawProjection(int rownum,int projCount,Mat image) {
//        final Point pt1 = new Point(0, -1);
//        final Point pt2 = new Point();
//        pt1.y = rownum;
//        pt2.x = projCount;
//        pt2.y = rownum;
//        Core.line(image, pt1, pt2, COLOR_GREEN);
//    }

    private static int rotate(double y1, double x1, int theta, Mat mat) {
        int x0 = mat.cols() / 2;
        int y0 = mat.rows() / 2;

        int new_col = (int) ((x1 - x0) * Math.cos(Math.toRadians(theta)) - (y1 - y0) * Math.sin(Math.toRadians(theta)) + x0);
        int new_row = (int) ((x1 - x0) * Math.sin(Math.toRadians(theta)) + (y1 - y0) * Math.cos(Math.toRadians(theta)) + y0);

        return new_row;

    }

    public class ClientThread implements Runnable {
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Log.d("ClientActivity", "C: Connecting...");
                socket = new Socket(serverAddr, SERVERPORT);
                clientConnected = true;
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
                    clientConnected = false;
                }
                socket.close();
                Log.d("ClientActivity", "C: Closed.");
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error", e);
                clientConnected = false;
            }
        }

        protected void onStop() {
            try {
                // MAKE SURE YOU CLOSE THE SOCKET UPON EXITING
                socket.close();
                clientConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ServerThread implements Runnable {
        private BufferedReader input;
        Handler handler = new Handler();

        public void run() {
            synchronized (this) {
                Socket socket = null;

                try {
                    serverSocket = new ServerSocket(SERVERPORT);
                    serverConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    serverConnected = false;
                }
                StringBuilder stringBuilder = new StringBuilder();
                while (!Thread.currentThread().isInterrupted() && serverConnected) {

                    try {

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

                    } catch (IOException e) {
                        e.printStackTrace();
                        serverConnected = false;
                    }
                }
                try {

//                   serverSocket.close();
                    serverConnected = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        protected void onStop() {
            try {
                // MAKE SURE YOU CLOSE THE SOCKET UPON EXITING
                Log.i("Server Thread:", "Stopped!");
                serverSocket.close();
                serverConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    ocrOutput = input.readLine();
                    Log.i("OCROutput:", ocrOutput);
//                    updateConversationHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

//        class updateUIThread implements Runnable {
//            private String msg;
//
//            public updateUIThread(String str) {
//                this.msg = str;
//            }
//
//            @Override
//            public void run() {
//                text.setText(text.getText().toString()+"Client Says: "+ msg + "\n");
//            }
//        }



    public byte[] getBytesFromBitmap(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
    }


}

