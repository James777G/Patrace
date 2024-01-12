package com.arm.pa.paretrace.Util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZipHandler {

    private static final String TAG = "GZipHandler";

    public static File gzipFile(File sourceFile) {

        Log.i(TAG, "Start of zipping file " + sourceFile.getName());

        File destinationFile = new File(sourceFile.getParent(), sourceFile.getName() + ".gz");

        byte[] buffer = new byte[1024];
        try (FileInputStream fileIn = new FileInputStream(sourceFile);
             GZIPOutputStream gZIPout = new GZIPOutputStream(new FileOutputStream(destinationFile))) {

            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) > 0) {
                gZIPout.write(buffer, 0, bytesRead);
            }

            Log.i(TAG, "The file is compressed successfully");
            return destinationFile;

        } catch (IOException ex) {
            Log.e(TAG, "Error occurred during file compression: " + ex.getMessage());
            return null;
        }
    }
}
