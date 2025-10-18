package com.securevault.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LocaleManager {

    private static Locale currentLocale = new Locale("tr");
    private static ResourceBundle bundle = loadBundle(currentLocale);

    public static void setLocale(String langCode) {
        currentLocale = new Locale(langCode);
        bundle = loadBundle(currentLocale);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "[" + key + "]";
        }
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    private static ResourceBundle loadBundle(Locale locale) {
        try {
            // UTF-8 kontrolü ile bundle yükle
            return ResourceBundle.getBundle("lang.messages", locale, new UTF8Control());
        } catch (Exception e) {
            System.err.println("Error loading resource bundle for locale: " + locale);
            e.printStackTrace();
            // Fallback: İngilizce dil paketini dene
            try {
                return ResourceBundle.getBundle("lang.messages", new Locale("en"), new UTF8Control());
            } catch (Exception ex) {
                return createEmptyBundle();
            }
        }
    }

    private static ResourceBundle createEmptyBundle() {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return "[" + key + "]";
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        };
    }

    // UTF-8 destekli ResourceBundle kontrolü
    public static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            InputStream stream = null;
            if (reload) {
                java.net.URL url = loader.getResource(resourceName);
                if (url != null) {
                    java.net.URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }

            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            } else {
                return null;
            }
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return locale.equals(Locale.ENGLISH) ? null : Locale.ENGLISH;
        }
    }
}