package com.securevault.utils;

import java.io.*;
import java.util.Properties;

public class AppPreferences {
    private static final String PREF_FILE = "preferences.properties";
    private static Properties props = new Properties();

    static {
        try (InputStream in = new FileInputStream(PREF_FILE)) {
            props.load(in);
        } catch (IOException e) {
            props.setProperty("app.language", "tr");
        }
    }

    public static String getLanguage() {
        return props.getProperty("app.language", "tr");
    }

    public static void setLanguage(String lang) {
        props.setProperty("app.language", lang);
        try (OutputStream out = new FileOutputStream(PREF_FILE)) {
            props.store(out, "App Preferences");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
