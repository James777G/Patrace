package com.arm.pa.paretrace.Util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CPUFreqReader {

    private static final String TAG = "CpuFrequencyReader";

    private static final int TIME_GAP = 1000;

    public static boolean isFluctuated = false;

    private static final int FREQUENCY_FLUCTUATION_THRESHOLD_PERCENT = 5;
    private final Context context;
    private final Handler handler = new Handler();
    private final Runnable instructions = new Runnable() {
        @Override
        public void run() {
            // Call the method to get CPU frequencies
            getAllCpuFrequencies(context);

            // Repeat this the same runnable code block again in 3 seconds
            handler.postDelayed(this, TIME_GAP);
        }
    };

    private static int numCores = 0;

    private static Integer[] frequenciesList;

    public CPUFreqReader(Context context){
        this.context = context;
    }

    public void startRecording() {
        // Start the initial runnable task by posting through the handler
        handler.post(instructions);
    }

    public void stopRecording() {
        // Remove callbacks and messages
        handler.removeCallbacks(instructions);
    }

    private void getAllCpuFrequencies(Context context) {
        File file = new File(context.getExternalCacheDir(), "cpu_frequencies.txt");

        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StrictMode.setThreadPolicy(
                        new StrictMode.ThreadPolicy.Builder()
                                .permitDiskReads()
                                .permitDiskWrites()
                                .permitNetwork()
                                .build()
                );
            }

            int numCores = Runtime.getRuntime().availableProcessors();
            if (frequenciesList == null){
                frequenciesList = new Integer[numCores];
            }

            for (int i = 0; i < numCores; i++) {
                String cpuPath = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                File cpuInfoFile = new File(cpuPath);
                long currentTime = System.currentTimeMillis(); // Unix timestamp

                if (cpuInfoFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(cpuInfoFile))) {
                        String frequency = reader.readLine().trim();
                        int currentFrequency = Integer.parseInt(frequency);
                        if(frequenciesList[i] == null){
                            frequenciesList[i] = currentFrequency;
                        } else {
                            int previousFrequency = frequenciesList[i];
                            int frequencyFluctuation = Math.abs(currentFrequency - previousFrequency);

                            // Check if the frequency fluctuation exceeds the threshold
                            if (((double) frequencyFluctuation / previousFrequency) * 100 > FREQUENCY_FLUCTUATION_THRESHOLD_PERCENT) {
                                // Frequency fluctuation is significant, mark the data as invalid
                                Log.d(TAG, "Frequency fluctuation for core " + i + " exceeds threshold. Invalidating data.");
                                isFluctuated = true;
                                createDummyFile(context);
                            }
                        }

                        String logEntry = currentTime + " - " + i + " - " + frequency;
                        writer.write(logEntry + "\n");
              //          Log.d(TAG, logEntry);
                    }
                } else {
                    String logEntry = currentTime + " - " + i + " - N/A";
                    writer.write(logEntry + "\n");
                    Log.d(TAG, logEntry);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file or reading CPU frequencies", e);
        }
    }


    private void createDummyFile(Context context) {
        File dummyFile = new File(context.getExternalCacheDir(), "Invalid.txt");

        try {
            if (dummyFile.createNewFile()) {
                Log.d(TAG, "Dummy file created to indicate frequency fluctuation.");
            } else {
                Log.d(TAG, "Dummy file creation failed. There might be an issue.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating dummy file", e);
        }
    }


}