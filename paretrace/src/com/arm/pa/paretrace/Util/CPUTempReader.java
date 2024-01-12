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
import java.util.Locale;
import java.util.Date;

public class CPUTempReader {

    private static final String TAG = "CpuTemperatureReader";
    private final Context context;
    private final Handler handler = new Handler();
    private final Runnable instructions = new Runnable() {
        @Override
        public void run() {
            // Call the method to read CPU temperatures
            readCpuTemperatures(context);

            // Repeat this the same runnable code block again in 3 seconds
            handler.postDelayed(this, 3000);
        }
    };

    public CPUTempReader(Context context) {
        this.context = context;
    }

    public void startRecording() {
        // Start the initial runnable task by posting through the handler
//        handler.post(instructions);
    }

    public void stopRecording() {
        // Remove callbacks and messages
//        handler.removeCallbacks(instructions);
    }

    private void readCpuTemperatures(Context context) {
        File file = new File(context.getExternalCacheDir(), "cpu_temperatures.txt");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        final int WARNING_THRESHOLD = 60;

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

            String[] temperaturePaths = {
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
                    "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
                    "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
                    "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
                    "/sys/devices/platform/tegra_tmon/temp1_input",
                    "/sys/devices/platform/s5p-tmu/temperature",
                    "/sys/devices/platform/s5p-tmu/curr_temp",
                    "/sys/devices/virtual/thermal/thermal_zone1/temp",
                    "/sys/devices/virtual/thermal/thermal_zone0/temp",
                    "/sys/class/thermal/thermal_zone0/temp",
                    "/sys/class/thermal/thermal_zone1/temp",
                    "/sys/class/thermal/thermal_zone3/temp",
                    "/sys/class/thermal/thermal_zone4/temp",
                    "/sys/class/hwmon/hwmon0/device/temp1_input",
                    "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
                    "/sys/kernel/debug/tegra_thermal/temp_tj",
                    "/sys/htc/cpu_temp",
                    "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/ext_temperature",
                    "/sys/devices/platform/tegra-tsensor/tsensor_temperature"
            };

            for (String path : temperaturePaths) {
                File temperatureFile = new File(path);
                String currentTime = dateFormat.format(new Date());

                if (temperatureFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(temperatureFile))) {
                        String temperature = reader.readLine().trim();
                        int temperatureInt = Integer.parseInt(temperature);
                        writer.write(currentTime + " - Temperature at " + path + ": " + temperature + "\n");
                        Log.d(TAG, "Temperature at " + path + ": " + temperature);

                        // Check if temperature exceeds threshold
                        if (temperatureInt > WARNING_THRESHOLD) {
                            String warningMessage = currentTime + " - WARNING: High temperature at " + path + ": " + temperature + "\n";
                            writer.write(warningMessage);
                            Log.w(TAG, warningMessage);  // Log warning with Log.w (warning)
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing temperature from " + path, e);
                    }
                } else {
                    writer.write(currentTime + " - Temperature information not available at " + path + "\n");
                    Log.d(TAG, "Temperature information not available at " + path);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file or reading CPU temperatures", e);
        }
    }
}