package com.dart.cameralibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.zip.Inflater;

import static android.content.ContentValues.TAG;

public class CameraActivity extends AppCompatActivity {

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {

                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        //setContentView(R.layout.activity_camera);

        final Button runButton = (Button) findViewById(R.id.start_button);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setContentView(R.layout.activity_camera);
                final Button button = (Button) findViewById(R.id.OKbutton);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
            }
        });

        final Button aboutButton = (Button) findViewById(R.id.about_button);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Developed by KUMAIL, NOOR, FERJAD!", Toast.LENGTH_SHORT).show();

            }
        });

        final Button ipButton = (Button) findViewById(R.id.ip_set);
        ipButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Server IP Set!", Toast.LENGTH_SHORT).show();

            }
        });




    }
    @Override
    protected void onResume(){
        super.onResume();
    }

    public void onBackPressed(){
        Intent mStartActivity = new Intent(CameraActivity.this, CameraActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(CameraActivity.this, mPendingIntentId, mStartActivity,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) CameraActivity.this.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }
}

