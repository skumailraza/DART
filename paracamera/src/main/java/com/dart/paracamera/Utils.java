package com.dart.paracamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class providing helper methods for file operations, bitmap decoding,
 * image rotation, and saving bitmaps. This class is used by the ParaCamera library.
 */
public class Utils {

    /**
     * Creates an image file in the application's private file storage.
     * It first ensures the specified directory exists, then creates the new file.
     *
     * @param context  The application or activity context.
     * @param dirName  The name of the directory within the app's files directory where the image should be stored.
     * @param fileName The name of the image file (without extension).
     * @param fileType The file extension (e.g., ".jpg", ".png").
     * @return A {@link File} object representing the created image file, or null if file creation fails.
     */
    public static File createImageFile(
            Context context,
            String dirName,
            String fileName,
            String fileType) {
        try {
            File file = createDir(context, dirName);
            File image = new File(file.getAbsoluteFile() + File.separator + fileName + fileType);
            if (!image.getParentFile().exists()) {
                image.getParentFile().mkdirs();
            }
            image.createNewFile();
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a directory within the application's private files directory if it doesn't already exist.
     *
     * @param context The application or activity context.
     * @param dirName The name of the directory to create.
     * @return A {@link File} object representing the directory.
     */
    public static File createDir(Context context, String dirName) {
        File file = new File(context.getFilesDir() + File.separator + dirName);
        // Create the directory if it does not exist.
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    /**
     * Decodes an image file into a {@link Bitmap}, scaling it down to approximately the required height
     * while maintaining aspect ratio. This method is optimized to reduce memory usage by first
     * checking the image dimensions and then decoding a scaled-down version.
     *
     * @param file           The image file to decode.
     * @param requiredHeight The target height for the decoded bitmap. The actual height may vary slightly
     *                       due to the scaling factor being a power of 2.
     * @return The decoded {@link Bitmap}, or null if decoding fails or the file is not found.
     */
    public static Bitmap decodeFile(File file, int requiredHeight) {
        try {
            // First, decode with inJustDecodeBounds=true to check dimensions without loading the full bitmap.
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(file), null, o);

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requiredHeight.
            // This optimizes memory usage by loading a smaller version of the image.
            int scale = 1;
            while (o.outWidth / scale / 2 >= requiredHeight &&
                    o.outHeight / scale / 2 >= requiredHeight) {
                scale *= 2;
            }

            // Decode the bitmap with the calculated inSampleSize.
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(file), null, o2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves a {@link Bitmap} to a specified file path with the given image type and compression quality.
     *
     * @param bitmap      The {@link Bitmap} to save.
     * @param filePath    The absolute path where the bitmap should be saved.
     * @param imageType   The image format type (e.g., ".png", ".jpg"). Used to determine the compress format.
     * @param compression The compression quality (0-100) for JPEG images. This is ignored for PNG.
     */
    public static void saveBitmap(Bitmap bitmap, String filePath, String imageType, int compression) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filePath);
            // Determine the Bitmap.CompressFormat based on the imageType string.
            // PNG is lossless, so the compression factor is ignored.
            // JPG/JPEG uses the specified compression quality.
            String lowerCaseImageType = imageType.toLowerCase();
            if (lowerCaseImageType.contains("png")) {
                bitmap.compress(Bitmap.CompressFormat.PNG, compression, out);
            } else if (lowerCaseImageType.contains("jpg") || lowerCaseImageType.contains("jpeg")) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, compression, out);
            // Default to JPEG if type is unclear but saving is still attempted.
            // Consider throwing an IllegalArgumentException for unsupported types.
            } else {
                // Defaulting to JPEG if type is unknown. Add logging or error handling if necessary.
                bitmap.compress(Bitmap.CompressFormat.JPEG, compression, out);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log or handle exception appropriately.
        } finally {
            try {
                if (out != null) {
                    out.close(); // Ensure the output stream is closed.
                }
            } catch (IOException e) {
                e.printStackTrace(); // Log or handle exception during close.
            }
        }
    }
// Old switch statement logic for reference or if specific case handling (e.g. ".PNG") is strictly needed.
//            switch (imageType) {
//                case "png":
//                case "PNG":
//                case ".png":
//                    bitmap.compress(Bitmap.CompressFormat.PNG, compression, out);
//                    break;
//                case "jpg":
//                case "JPG":
//                case ".jpg":
//                case "jpeg":
//                case "JPEG":
//                case ".jpeg":
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, compression, out);
//                    break;
//            }

    /**
     * Reads the EXIF orientation tag from an image file and converts it into a rotation angle in degrees.
     *
     * @param imagePath The absolute path to the image file.
     * @return The rotation angle (0, 90, 180, or 270 degrees), or 0 if orientation cannot be determined or an error occurs.
     */
    public static int getImageRotation(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                ExifInterface exif = new ExifInterface(imageFile.getPath());
                // Get the orientation tag from EXIF metadata.
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                // Convert EXIF orientation to degrees.
                return exifToDegrees(rotation);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log or handle exception.
        }
        return 0; // Default to 0 degrees if orientation cannot be read.
    }

    /**
     * Rotates a {@link Bitmap} by the specified angle in degrees.
     *
     * @param src      The source {@link Bitmap} to rotate.
     * @param rotation The rotation angle in degrees.
     * @return The rotated {@link Bitmap}. If rotation is 0, the original bitmap is returned.
     */
    public static Bitmap rotateBitmap(Bitmap src, int rotation) {
        Matrix matrix = new Matrix();
        if (rotation != 0) {
            // Apply the rotation to the matrix.
            matrix.preRotate(rotation);
            // Create a new bitmap by transforming the source bitmap using the rotation matrix.
            return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        }
        return src; // Return the original bitmap if no rotation is needed.
    }

    /**
     * Converts EXIF orientation constants to rotation angles in degrees.
     *
     * @param exifOrientation The EXIF orientation constant (e.g., {@link ExifInterface#ORIENTATION_ROTATE_90}).
     * @return The corresponding angle in degrees (0, 90, 180, or 270).
     */
    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        // Default to 0 degrees for ORIENTATION_NORMAL or undefined orientations.
        return 0;
    }
}
