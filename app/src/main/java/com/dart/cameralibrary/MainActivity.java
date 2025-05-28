package com.dart.cameralibrary;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.dart.paracamera.Camera;

/**
 * An alternative activity for capturing and displaying an image using the ParaCamera library.
 * This activity directly initiates picture taking upon creation and displays the result.
 * It appears to be a simpler or perhaps test-oriented version compared to {@link CameraActivity}.
 */
public class MainActivity extends AppCompatActivity {
    private ImageView picFrame; // ImageView to display the captured picture.
    private Camera camera;      // Instance of the ParaCamera library.

    /**
     * Called when the activity is first created.
     * Initializes the layout and the ImageView for displaying the picture.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        picFrame = (ImageView) findViewById(R.id.picFrame);
    }

    /**
     * Called when activity start-up is complete (after {@link #onStart} and
     * {@link #onRestoreInstanceState(Bundle)} have been called).
     * This is where the ParaCamera instance is built and picture taking is initiated.
     * Using onPostCreate for camera initialization and picture taking ensures that
     * the activity is fully visible and set up before the camera action starts.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Initialize ParaCamera with desired settings.
        camera = new Camera.Builder()
                .setDirectory("pics") // Directory to save the image.
                .setName("ali_" + System.currentTimeMillis()) // Unique image name.
                .setImageFormat(Camera.IMAGE_JPEG) // Image format.
                .setCompression(75) // Compression quality.
                .setImageHeight(1000) // Target image height.
                .build(this); // Build with the current activity as context.
        try {
            // Trigger camera to take a picture.
            camera.takePicture();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.
     * This is called when the camera activity (started by ParaCamera) returns its result.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if the result is from the ParaCamera's photo request.
        if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
            // Get the captured bitmap from ParaCamera.
            Bitmap bitmap = camera.getCameraBitmap();

            if (bitmap != null) {
                // If bitmap is successfully captured, display it in the ImageView.
                picFrame.setImageBitmap(bitmap);
            } else {
                // If bitmap is null, show a toast message.
                Toast.makeText(this.getApplicationContext(), "Picture not taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed. The counterpart to
     * {@link #onResume}.
     * When an activity is paused, it should release any resources that should
     * not be held while the activity is not active, such as camera resources.
     * However, ParaCamera manages its own resources; here we only delete the image if needed.
     */
    // No onPause override in original, added for completeness if resource handling was intended here.
    // For now, will comment out as it wasn't in the original and onDestroy handles image deletion.
//    @Override
//    protected void onPause() {
//        super.onPause();
//        // If you need to release camera immediately or stop previews, do it here.
//        // ParaCamera might handle this internally upon activity pause.
//    }

    /**
     * Perform any final cleanup before an activity is destroyed.
     * This can happen either because the activity is finishing (someone called
     * {@link #finish} on it, or because the system is temporarily destroying
     * this instance of the activity to save space.
     * Here, it's used to delete the captured image file managed by ParaCamera.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Delete the image file associated with the ParaCamera instance.
        // This is important for cleaning up storage.
        if (camera != null) {
            camera.deleteImage();
        }
    }
}
