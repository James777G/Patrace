package com.arm.pa.paretrace.Sources;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.arm.pa.paretrace.Activities.SelectActivity;
import com.arm.pa.paretrace.Types.Trace;
import com.arm.pa.paretrace.Util.GZipHandler;
import com.arm.pa.paretrace.Util.YamlReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CloudTraceResourceLoader {
    private static final String TAG = "CloudTraceResource";
    private static final String HOST_NAME = "130.216.216.90:8080";

    private static final String RELEASE_VERSION = Build.VERSION.RELEASE;


    private static String FILE_URL = "http://130.216.216.90/Puzzle/com.block.juggle.1.pat";

    private static final String MANUFACTURER = Build.MANUFACTURER;
    private static final String MODEL = Build.MODEL;

    private static final String BRAND = Build.BRAND;


    private static String TESTING_DEVICE = "LOCAL_NEW2";

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/octet-stream");




    public void downloadFiles(Context context, ServerFilesInfo serverInfo) {
        String baseUrl = serverInfo.getHostname() + ":" + serverInfo.getPort();
        for (Map.Entry<String, List<String>> entry : serverInfo.getFiles().entrySet()) {
            String genre = entry.getKey();
            for (String path : entry.getValue()) {
                String fullUrl = baseUrl + "/" + genre + path;
                downloadFile(fullUrl, context);
            }
        }
    }



    public void downloadSingleTrace(final List<Trace> traceList, final UpdatableUI updatableUI, final Context context) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Start downloading trace file");
                    OkHttpClient client = new OkHttpClient();
                    String credential = Credentials.basic("admin", "openglfarm");
                    if(!SelectActivity.FILE_URL.isEmpty()){
                        FILE_URL = SelectActivity.FILE_URL;
                    }

                    Request request = new Request.Builder()
                            .url(FILE_URL)
                            .header("Authorization", credential)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Failed to download file: HTTP error code: " + response.code());
                            throw new RuntimeException("Failed to download file: HTTP error code: " + response.code());
                        }

                        ResponseBody body = response.body();
                        if (body == null) {
                            Log.e(TAG, "Response body is null");
                            throw new RuntimeException("Response body is null");
                        }

                        final int fileLength = (int) body.contentLength();
                        if(fileLength < 0){
                            Log.e(TAG, "The file length is negative");
                        }
                        int totalDownloaded = 0;

                        InputStream inputStream = new BufferedInputStream(body.byteStream());
                        File file = new File(context.getFilesDir(), "downloadedFile.pat");
                        FileOutputStream fileOutputStream = new FileOutputStream(file);

                        byte[] data = new byte[1024];
                        int count;

                        while ((count = inputStream.read(data)) != -1) {
                            fileOutputStream.write(data, 0, count);
                            totalDownloaded += count;
                            final int progress = (int) ((totalDownloaded / (float) fileLength) * 100);

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    // Update the UI with the progress
                                    updatableUI.updateProgress(progress, fileLength);
                                }
                            });
                        }

                        Trace t = new Trace();
                        t.setFile(file);
                        traceList.add(t);

                        fileOutputStream.close();
                        inputStream.close();

                        Log.i(TAG, "Download Finished");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                // Update UI
                                updatableUI.update();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(getClass().getCanonicalName(), "Exception Occurred During Network Request");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // Update UI
                            updatableUI.updateFailureMessage();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void downloadFile(String s, Context context){
        // TODO (MAYBE NOT NECESSARY)
    }

    public static void setFileUrl(String fileUri) {
        FILE_URL = fileUri;
    }

    public static String processUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();

            // Remove the leading '/' and replace all '/' with '-'
            return path.substring(1).replace("/", "-");
        } catch (MalformedURLException e) {
            // Handle the error appropriately
            System.err.println("Invalid URL: " + e.getMessage());
            return null;
        }
    }

    public static void uploadFile(final File file, final Context context) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerFilesInfo serverFilesInfo = YamlReader.readYamlFile(context);
                if(serverFilesInfo == null) {
                    Log.e(TAG, "Server files info is null");
                }

                Log.i(TAG, "Server Info: " + serverFilesInfo);
                Log.i(TAG, "Start of uploading log files");
                File externalCacheDir = context.getExternalCacheDir();
                if(externalCacheDir!= null && externalCacheDir.isDirectory()) {
                    File[] files = externalCacheDir.listFiles();
                    while(!isLoggingFinished(files)) {
                        Log.i(TAG, "The logging is not finished yet");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        externalCacheDir = context.getExternalCacheDir();
                        files = externalCacheDir.listFiles();
                    }
                }
                Log.i(TAG, "Start processing " + file.getName());
                OkHttpClient client = new OkHttpClient();
                File file1 = GZipHandler.gzipFile(file);
                assert file1 != null;
                Log.i(TAG, "the size of " + file1.getName() + " is " + file1.length());

                assert serverFilesInfo != null;
                String credential = Credentials.basic(serverFilesInfo.getUsername(), serverFilesInfo.getPassword());

                RequestBody requestBody = RequestBody.create(file1, MEDIA_TYPE);

                if(file.getName().equals("Invalid.txt")){
                    Log.i(TAG, "Invalid txt found");
                    Request request = new Request.Builder()
                            .url(serverFilesInfo.getFullHostName() + "/" + serverFilesInfo.getInvalidFile() + "/" + BRAND + "_" + MODEL + "_" + MANUFACTURER + "_" + RELEASE_VERSION + "/" + TESTING_DEVICE + "/" + file1.getName())
                            .put(requestBody)
                            .header("Authorization", credential)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        assert response.body() != null;
                        Log.i(TAG, "Printing the response body: " + response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, "Error occurred during uploading invalid files");
                        e.printStackTrace();
                    }

                }

                if(!SelectActivity.TESTING_DEVICE.isEmpty()){
                    TESTING_DEVICE = SelectActivity.TESTING_DEVICE;
                }

                Request request = new Request.Builder()
                        .url(serverFilesInfo.getFullHostName() + "/" + serverFilesInfo.getCloudDir() + "/" + BRAND + "/" + MODEL + "-" + MANUFACTURER + "/" + RELEASE_VERSION + "/" + TESTING_DEVICE + "/" + processUrl(FILE_URL) + "/" + file1.getName())
                        .put(requestBody)
                        .header("Authorization", credential)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    assert response.body() != null;
                    Log.i(TAG, "Printing the response body: " + response.body().string());
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred during uploading");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static boolean isLoggingFinished(File[] files) {
        for(File file : files) {
            Log.i(TAG, "file " + file.getName() + " is in the directory");
            if("done.bin".equals(file.getName())){
                return true;
            }
        }
        return false;
    }


}