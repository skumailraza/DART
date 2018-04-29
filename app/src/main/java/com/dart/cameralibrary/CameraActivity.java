package com.dart.cameralibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.dart.paracamera.Camera;

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
        final RadioGroup lang = (RadioGroup)findViewById(R.id.LangGroup);
        lang.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.English)
                    CameraFragment.lFlag = true;
                if (checkedId == R.id.Urdu)
                    CameraFragment.lFlag = false;
            }
        });

        final ImageButton runButton = (ImageButton) findViewById(R.id.start_button);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setContentView(R.layout.activity_camera);
                final Button button = (Button) findViewById(R.id.OKbutton);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent mStartActivity = new Intent(CameraActivity.this, CameraActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(CameraActivity.this, mPendingIntentId, mStartActivity,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) CameraActivity.this.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10, mPendingIntent);
                        System.exit(0);
                    }
                });
            }
        });

        final ImageButton aboutButton = (ImageButton) findViewById(R.id.about_button);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Developed by KUMAIL, NOOR, FERJAD!", Toast.LENGTH_SHORT).show();

            }
        });

        final Button ipButton = (Button) findViewById(R.id.ip_set);
        ipButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Server IP Set!", Toast.LENGTH_SHORT).show();
                CameraFragment.SERVER_IP = findViewById(R.id.ip_address).toString();
            }
        });




    }
    @Override
    protected void onResume(){
        super.onResume();
    }

    public void onBackPressed(){
        this.finish();
    }
}

