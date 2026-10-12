package com.personaltracker.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application Configuration manager.
 * Loads settings from environment variables, .env file, or runtime configuration.
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static AppConfig instance;

    private String supabaseUrl;
    private String supabaseAnonKey;
    private String dbHost;
    private int dbPort = 5432;
    private String dbName = "postgres";
    private String dbUser = "postgres";
    private String dbPassword = "";
    private boolean mockMode = false;

    private static final String CONFIG_FILE = "personal_tracker.properties";

    private AppConfig() {
        loadConfiguration();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public void loadConfiguration() {
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
        } catch (DotenvException e) {
            LOGGER.log(Level.INFO, "No local .env file found, falling back to system environment variables.");
        }

        this.supabaseUrl = getEnvValue("SUPABASE_URL", dotenv, "");
        this.supabaseAnonKey = getEnvValue("SUPABASE_ANON_KEY", dotenv, "");
        this.dbHost = getEnvValue("DB_HOST", dotenv, "");
        String portStr = getEnvValue("DB_PORT", dotenv, "5432");
        try {
            this.dbPort = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {}

        this.dbName = getEnvValue("DB_NAME", dotenv, "postgres");
        this.dbUser = getEnvValue("DB_USER", dotenv, "postgres");
        this.dbPassword = getEnvValue("DB_PASSWORD", dotenv, "");

        // Load overrides from user property file if present
        File propFile = new File(CONFIG_FILE);
        if (propFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(propFile)) {
                props.load(in);
                if (props.getProperty("SUPABASE_URL") != null) {
                    this.supabaseUrl = props.getProperty("SUPABASE_URL");
                }
                if (props.getProperty("SUPABASE_ANON_KEY") != null) {
                    this.supabaseAnonKey = props.getProperty("SUPABASE_ANON_KEY");
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read property file: " + e.getMessage());
            }
        }

        // If credentials are empty or default placeholder, enable mock/offline mode
        if (isPlaceholderOrEmpty(this.supabaseUrl) || isPlaceholderOrEmpty(this.supabaseAnonKey)) {
            LOGGER.info("Supabase credentials not configured. Running in local in-memory demo mode.");
            this.mockMode = true;
        } else {
            this.mockMode = false;
        }
    }

    public void saveRuntimeCredentials(String url, String anonKey) {
        this.supabaseUrl = url != null ? url.trim() : "";
        this.supabaseAnonKey = anonKey != null ? anonKey.trim() : "";
        this.mockMode = isPlaceholderOrEmpty(this.supabaseUrl) || isPlaceholderOrEmpty(this.supabaseAnonKey);

        Properties props = new Properties();
        props.setProperty("SUPABASE_URL", this.supabaseUrl);
        props.setProperty("SUPABASE_ANON_KEY", this.supabaseAnonKey);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Personal Gain Tracking System Runtime Config");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not persist credentials to file: " + e.getMessage());
        }
    }

    private String getEnvValue(String key, Dotenv dotenv, String defaultValue) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val.trim();

        if (dotenv != null) {
            String dotVal = dotenv.get(key);
            if (dotVal != null && !dotVal.isBlank()) return dotVal.trim();
        }

        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) return sysProp.trim();

        return defaultValue;
    }

    private boolean isPlaceholderOrEmpty(String val) {
        if (val == null || val.isBlank()) return true;
        return val.contains("your-project-id") || val.contains("your-anon-key-here");
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getSupabaseAnonKey() {
        return supabaseAnonKey;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public boolean isSupabaseConfigured() {
        return !mockMode && supabaseUrl != null && !supabaseUrl.isBlank() && supabaseAnonKey != null && !supabaseAnonKey.isBlank();
    }
}
