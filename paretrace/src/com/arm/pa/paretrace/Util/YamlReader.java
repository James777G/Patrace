package com.arm.pa.paretrace.Util;

import android.content.Context;

import com.arm.pa.paretrace.Sources.ServerFilesInfo;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class YamlReader {

    public static ServerFilesInfo readYamlFile(Context context) {
        Yaml yaml = new Yaml();
        try {
            InputStream inputStream = context.getAssets().open("file_list.yaml");
            return yaml.loadAs(inputStream, ServerFilesInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
