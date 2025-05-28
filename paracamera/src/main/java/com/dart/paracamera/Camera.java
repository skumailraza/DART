package com.dart.paracamera;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

/**
 * Manages camera operations such as capturing pictures using the device's built-in camera application.
 * It handles file creation, intent setup, and processing of the captured image,
 * including resizing, rotation correction, and compression.
 * This class uses a Builder pattern for its instantiation.
 */
public class Camera {

    // Public constants for image formats
    /** Constant for JPG image format. */
    public static final String IMAGE_JPG = "jpg";
    /** Constant for JPEG image format. */
    public static final String IMAGE_JPEG = "jpeg";
    /** Constant for PNG image format. */
    public static final String IMAGE_PNG = "png";

    // Default values used by the camera settings
    private static final String IMAGE_FORMAT_JPG = ".jpg";
    private static final String IMAGE_FORMAT_JPEG = ".jpeg";
    private static final String IMAGE_FORMAT_PNG = ".png";
    private static final int IMAGE_HEIGHT = 1000; // Default target height for the image
    private static final int IMAGE_COMPRESSION = 75; // Default JPEG compression quality
    private static final String IMAGE_DEFAULT_DIR = "capture"; // Default directory to save images
    private static final String IMAGE_DEFAULT_NAME = "img_"; // Default prefix for image filenames

    /**
     * Request code used when starting the camera intent via {@link Activity#startActivityForResult(Intent, int)}.
     * This can be customized via the {@link Builder#setTakePhotoRequestCode(int)}.
     */
    public static int REQUEST_TAKE_PHOTO = 1234;

    // Member variables
    private Context context;
    private Activity activity;
    private Fragment fragment;
    private android.support.v4.app.Fragment compatFragment;
    private String cameraBitmapPath = null;
    private Bitmap cameraBitmap = null;
    private String dirName;
    private String imageName;
    private String imageType;
    private int imageHeight;
    private int compression;
    private boolean isCorrectOrientationRequired;
    private MODE mode;

    private String authority; // FileProvider authority string.

    /**
     * Private constructor to initialize Camera instance using the Builder.
     * @param builder The {@link Builder} instance containing all configuration settings.
     */
    private Camera(Builder builder) {
        activity = builder.activity;
        context = builder.context;
        mode = builder.mode;
        fragment = builder.fragment;
        compatFragment = builder.compatFragment;
        dirName = builder.dirName;
        REQUEST_TAKE_PHOTO = builder.REQUEST_TAKE_PHOTO;
        imageName = builder.imageName;
        imageType = builder.imageType;
        isCorrectOrientationRequired = builder.isCorrectOrientationRequired;
        compression = builder.compression;
        imageHeight = builder.imageHeight;
        authority = context.getApplicationContext().getPackageName() + ".imageprovider"; // Construct FileProvider authority.
    }

    /**
     * Sets up the camera intent with the necessary extras for capturing and saving an image.
     * This includes creating an image file, generating a content URI via FileProvider,
     * and granting URI permissions to camera apps that can handle the intent.
     *
     * @param takePictureIntent The {@link Intent#ACTION_IMAGE_CAPTURE} intent to configure.
     * @throws NullPointerException if the image file cannot be created.
     */
    private void setUpIntent(Intent takePictureIntent){
        // Create the file where the photo should go.
        File photoFile = Utils.createImageFile(context, dirName, imageName, imageType);
        if (photoFile != null) {
            cameraBitmapPath = photoFile.getAbsolutePath(); // Store the absolute path to the image file.

            // Get a content URI for the file using FileProvider to securely share it.
            Uri uri = FileProvider.getUriForFile(context, authority, photoFile);

            // Set the EXTRA_OUTPUT parameter to tell the camera app to save the image to this URI.
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            // Grant temporary read/write permissions to any camera app that can handle this intent.
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
        else {
            throw new NullPointerException("Image file could not be created by Utils.createImageFile");
        }
    }

    /**
     * Initiates the process of taking a picture using an available camera application.
     * It creates an intent to capture an image, sets up the output file URI,
     * and starts the camera activity based on whether it's called from an Activity,
     * a native Fragment, or a support Fragment.
     *
     * @throws NullPointerException if the image file cannot be created (propagated from {@link #setUpIntent(Intent)}).
     * @throws IllegalAccessException if no camera application is available to handle the intent.
     */
    public void takePicture() throws NullPointerException, IllegalAccessException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        switch (mode) { // Determine how to launch the camera based on the calling context.
            case ACTIVITY:
                if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                    setUpIntent(takePictureIntent); // Configure intent with output URI and permissions.
                    activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } else {
                    throw new IllegalAccessException("No camera app available to handle ACTION_IMAGE_CAPTURE intent from Activity.");
                }
                break;

            case FRAGMENT:
                if (takePictureIntent.resolveActivity(fragment.getActivity().getPackageManager()) != null) {
                        setUpIntent(takePictureIntent);
                        fragment.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } else {
                    throw new IllegalAccessException("No camera app available to handle ACTION_IMAGE_CAPTURE intent from Fragment.");
                }
                break;

            case COMPAT_FRAGMENT: // Support library Fragment
                if (takePictureIntent.resolveActivity(compatFragment.getActivity().getPackageManager()) != null) {
                        setUpIntent(takePictureIntent);
                        compatFragment.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } else {
                    throw new IllegalAccessException("No camera app available to handle ACTION_IMAGE_CAPTURE intent from support Fragment.");
                }
                break;
        }
    }

    /**
     * Retrieves the file path of the captured and processed camera image.
     * This method first ensures the bitmap is processed (resized, rotated, compressed)
     * by calling {@link #getCameraBitmap()}, then recycles the bitmap and returns the path.
     *
     * @return The absolute file path of the saved camera image, or null if not available.
     */
    public String getCameraBitmapPath() {
        Bitmap bitmap = getCameraBitmap(); // Process and save the bitmap.
        if (bitmap != null) {
            bitmap.recycle(); // Recycle the bitmap as we only need the path now.
        }
        return cameraBitmapPath;
    }

    /**
     * Retrieves the captured image as a {@link Bitmap}, processed according to the
     * settings specified in the {@link Builder} (e.g., height, orientation, compression).
     *
     * @return The processed {@link Bitmap}, or null if an error occurs or no image is available.
     */
    public Bitmap getCameraBitmap() {
        // Resize and process the image using the default imageHeight set in the Builder.
        return resizeAndGetCameraBitmap(imageHeight);
    }

    /**
     * Resizes the captured image to the specified height (maintaining aspect ratio),
     * applies orientation correction and compression if specified, saves it, and returns its file path.
     *
     * @param imageHeight The target height for the image.
     * @return The absolute file path of the processed and saved image, or null if not available.
     */
    public String resizeAndGetCameraBitmapPath(int imageHeight) {
        Bitmap bitmap = resizeAndGetCameraBitmap(imageHeight); // Process and save the bitmap.
        if (bitmap != null) {
            bitmap.recycle(); // Recycle the bitmap.
        }
        return cameraBitmapPath;
    }

    /**
     * Resizes the captured image to the specified height, corrects its orientation if required,
     * compresses it, and saves it back to its original path.
     *
     * @param imageHeight The target height to resize the image to, maintaining aspect ratio.
     * @return The processed {@link Bitmap}, or null if an error occurs.
     */
    public Bitmap resizeAndGetCameraBitmap(int imageHeight) {
        try {
            // If a previously loaded bitmap exists, recycle it to free memory.
            if (cameraBitmap != null) {
                cameraBitmap.recycle();
            }

            // Decode the image file from path, scaling it down to near the required height.
            cameraBitmap = Utils.decodeFile(new File(cameraBitmapPath), imageHeight);

            if (cameraBitmap != null) {
                // Correct image orientation if required.
                if (isCorrectOrientationRequired) {
                    cameraBitmap = Utils.rotateBitmap(cameraBitmap, Utils.getImageRotation(cameraBitmapPath));
                }
                // Save the processed (resized, rotated) bitmap back to the file, applying compression.
                Utils.saveBitmap(cameraBitmap, cameraBitmapPath, imageType, compression);
            }
            return cameraBitmap;
        } catch (Exception e) {
            // Log error or handle appropriately
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes the image file that was captured by the camera.
     * It's good practice to call this when the image is no longer needed to free up storage.
     */
    public void deleteImage() {
        if (cameraBitmapPath != null) {
            File image = new File(cameraBitmapPath);
            if (image.exists()) {
                image.delete();
            }
        }
        // Optionally, also recycle and nullify cameraBitmap if it's holding the image data.
        if (cameraBitmap != null) {
            cameraBitmap.recycle();
            cameraBitmap = null;
        }
    }

    /**
     * Enum to specify the context from which the camera is being launched (Activity, Fragment, or support Fragment).
     * This helps in calling the appropriate `startActivityForResult` method.
     */
    private enum MODE {ACTIVITY, FRAGMENT, COMPAT_FRAGMENT}

    /**
     * Builder class for {@link Camera}. Provides a fluent API to configure and create a Camera instance.
     */
    public static class Builder {
        // Builder fields to store configuration settings.
        private Context context;
        private Activity activity;
        private Fragment fragment;
        private android.support.v4.app.Fragment compatFragment;
        private String dirName;
        private String imageName;
        private String imageType;
        private int imageHeight;
        private int compression;
        private boolean isCorrectOrientationRequired;
        private MODE mode;
        private int REQUEST_TAKE_PHOTO; // Request code for starting camera intent.

        /**
         * Default constructor for the Builder. Initializes settings to their default values.
         */
        public Builder() {
            // Initialize with default settings from the Camera class.
            dirName = Camera.IMAGE_DEFAULT_DIR;
            imageName = Camera.IMAGE_DEFAULT_NAME + System.currentTimeMillis(); // Ensure unique name by default.
            imageHeight = Camera.IMAGE_HEIGHT;
            compression = Camera.IMAGE_COMPRESSION;
            imageType = Camera.IMAGE_FORMAT_JPG; // Default to JPG.
            REQUEST_TAKE_PHOTO = Camera.REQUEST_TAKE_PHOTO; // Use default request code.
        }

        /**
         * Sets the directory name where the captured image will be saved.
         * If null is passed, the default directory "capture" is used.
         * The directory is created within the application's files directory.
         * @param dirName The name of the directory.
         * @return This Builder instance for chaining.
         */
        public Builder setDirectory(String dirName) {
            if (dirName != null)
                this.dirName = dirName;
            return this;
        }

        /**
         * Sets the request code to be used when starting the camera intent.
         * This code is returned in {@link Activity#onActivityResult(int, int, Intent)}.
         * @param requestCode The request code.
         * @return This Builder instance for chaining.
         */
        public Builder setTakePhotoRequestCode(int requestCode) {
            this.REQUEST_TAKE_PHOTO = requestCode;
            return this;
        }

        /**
         * Sets the name for the captured image file (without extension).
         * If null is passed, a default name with a timestamp is used.
         * @param imageName The base name for the image file.
         * @return This Builder instance for chaining.
         */
        public Builder setName(String imageName) {
            if (imageName != null)
                this.imageName = imageName;
            return this;
        }

        /**
         * Specifies whether the captured image's orientation should be automatically corrected
         * based on its EXIF data.
         * @param reset True to enable orientation correction, false otherwise.
         * @return This Builder instance for chaining.
         */
        public Builder resetToCorrectOrientation(boolean reset) {
            this.isCorrectOrientationRequired = reset;
            return this;
        }

        /**
         * Sets the desired image format for the saved picture.
         * Supports "png", "jpg", "jpeg" (case-insensitive, with or without leading dot).
         * Defaults to JPG if an invalid or empty format is provided.
         * @param imageFormat The image format string (e.g., "png", ".jpg").
         * @return This Builder instance for chaining.
         */
        public Builder setImageFormat(String imageFormat) {
            if (TextUtils.isEmpty(imageFormat)) {
                return this; // Keep default if empty or null
            }

            // Normalize format string by removing leading dot and converting to lowercase for comparison
            String normalizedFormat = imageFormat.startsWith(".") ? imageFormat.substring(1).toLowerCase() : imageFormat.toLowerCase();

            switch (normalizedFormat) {
                case IMAGE_PNG:
                    this.imageType = IMAGE_FORMAT_PNG;
                    break;
                case IMAGE_JPG: // Falls through
                case IMAGE_JPEG:
                    this.imageType = IMAGE_FORMAT_JPG; // Or IMAGE_FORMAT_JPEG, assuming they are equivalent for saving
                    break;
                default:
                    this.imageType = IMAGE_FORMAT_JPG; // Default to JPG for unrecognized formats
            }
            return this;
        }

        /**
         * Sets the target height for the captured image. The library will attempt to resize
         * the image to this height while maintaining its aspect ratio.
         * @param imageHeight The target image height in pixels.
         * @return This Builder instance for chaining.
         */
        public Builder setImageHeight(int imageHeight) {
            this.imageHeight = imageHeight;
            return this;
        }

        /**
         * Sets the compression quality for JPEG images.
         * The value should be between 0 (lowest quality, highest compression) and 100 (highest quality, lowest compression).
         * Values outside this range are clamped. This setting is ignored for PNG images.
         * @param compression The compression quality (0-100).
         * @return This Builder instance for chaining.
         */
        public Builder setCompression(int compression) {
            // Clamp compression value to the 0-100 range.
            if (compression > 100) {
                this.compression = 100;
            } else if (compression < 0) {
                this.compression = 0;
            } else {
                this.compression = compression;
            }
            return this;
        }

        /**
         * Builds and returns a {@link Camera} instance configured for use with an {@link Activity}.
         * @param activity The calling Activity.
         * @return A new configured {@link Camera} instance.
         */
        public Camera build(Activity activity) {
            this.activity = activity;
            this.context = activity.getApplicationContext(); // Use application context to avoid leaks.
            this.mode = MODE.ACTIVITY;
            return new Camera(this);
        }

        /**
         * Builds and returns a {@link Camera} instance configured for use with a native {@link Fragment}.
         * @param fragment The calling Fragment.
         * @return A new configured {@link Camera} instance.
         */
        public Camera build(Fragment fragment) {
            this.fragment = fragment;
            this.context = fragment.getActivity().getApplicationContext();
            this.mode = MODE.FRAGMENT;
            return new Camera(this);
        }

        /**
         * Builds and returns a {@link Camera} instance configured for use with a support library {@link android.support.v4.app.Fragment}.
         * @param fragment The calling support Fragment.
         * @return A new configured {@link Camera} instance.
         */
        public Camera build(android.support.v4.app.Fragment fragment) {
            this.compatFragment = fragment;
            this.context = fragment.getActivity().getApplicationContext();
            this.mode = MODE.COMPAT_FRAGMENT;
            return new Camera(this);
        }
    }
}

