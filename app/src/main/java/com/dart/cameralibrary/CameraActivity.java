package com.dart.cameralibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

/**
 * Main activity for the DART application.
 * This activity handles the main menu, language selection, and server IP configuration.
 * It also initializes OpenCV and sets up navigation to the camera functionality.
 */
public class CameraActivity extends AppCompatActivity {

    /**
     * Callback for OpenCV Manager initialization.
     * It handles the connection status to the OpenCV service.
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    // OpenCV loaded successfully
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * Called when the activity is first created.
     * This method initializes the UI, sets up listeners for UI elements,
     * and starts OpenCV initialization.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Asynchronously initialize OpenCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Language selection RadioGroup
        final RadioGroup lang = (RadioGroup)findViewById(R.id.LangGroup);
        lang.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                // Set language flag based on selection (true for English, false for Urdu)
                // This flag is used by CameraFragment to determine OCR language.
                if (checkedId == R.id.English)
                    CameraFragment.lFlag = true;
                if (checkedId == R.id.Urdu)
                    CameraFragment.lFlag = false;
            }
        });

        // Button to start the camera functionality
        final ImageButton runButton = (ImageButton) findViewById(R.id.start_button);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Switch to the camera activity layout
                setContentView(R.layout.activity_camera);
                final Button button = (Button) findViewById(R.id.OKbutton);
                // This nested listener seems to intend to restart the activity or a similar one.
                // It schedules an alarm to restart CameraActivity and then exits the current instance.
                // This is an unconventional way to manage activity flow.
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent mStartActivity = new Intent(CameraActivity.this, CameraActivity.class);
                        int mPendingIntentId = 123456; // Unique ID for the PendingIntent
                        PendingIntent mPendingIntent = PendingIntent.getActivity(CameraActivity.this, mPendingIntentId, mStartActivity,
                                PendingIntent.FLAG_CANCEL_CURRENT); // Flag to cancel existing intent with same ID
                        AlarmManager mgr = (AlarmManager) CameraActivity.this.getSystemService(Context.ALARM_SERVICE);
                        // Schedule the restart shortly after the current time
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10, mPendingIntent);
                        System.exit(0); // Exits the current application process. This is a hard exit.
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

        // Button to set the server IP address
        final Button ipButton = (Button) findViewById(R.id.ip_set);
        ipButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Server IP Set!", Toast.LENGTH_SHORT).show();
                // Sets the server IP address.
                // Note: findViewById(R.id.ip_address).toString() gets the string representation
                // of the View object itself, not its content (e.g., text from an EditText).
                // This should likely be ((EditText)findViewById(R.id.ip_address)).getText().toString()
                // if R.id.ip_address is an EditText.
                CameraFragment.SERVER_IP = findViewById(R.id.ip_address).toString();
            }
        });
    }

    /**
     * Called when the activity has detected the user's press of the back key.
     * The default implementation simply finishes the current activity,
     * but you can override this to do whatever you want.
     */
    public void onBackPressed(){
        // Finishes the current activity, effectively going back to the previous screen or exiting.
        this.finish();
    }
}

