package com.myapitutorial.core.config;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import jakarta.inject.Singleton;

@Singleton
@Service
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private final Map<String, Object> config;
    
    public AppConfig() {
        this.config = loadConfiguration();
    }
    
    private Map<String, Object> loadConfiguration() {
        String environment = Optional.ofNullable(System.getenv("APP_ENV"))
                .orElse(Optional.ofNullable(System.getProperty("app.env"))
                .orElse("dev"));
        
        Yaml yaml = new Yaml();
        
        // Load base configuration first
        Map<String, Object> baseConfig = loadConfigFile(yaml, "application.yml");
        
        // Load environment-specific configuration
        String envConfigFile = "application-" + environment + ".yml";
        Map<String, Object> envConfig = loadConfigFile(yaml, envConfigFile);
        
        // Merge configurations (environment overrides base)
        if (baseConfig != null && envConfig != null) {
            logger.info("Merging base configuration with environment: {}", environment);
            return mergeConfigs(baseConfig, envConfig);
        } else if (envConfig != null) {
            logger.info("Using environment configuration: {}", environment);
            return envConfig;
        } else if (baseConfig != null) {
            logger.info("Using base configuration only");
            return baseConfig;
        } else {
            logger.error("No configuration files found!");
            throw new RuntimeException("Configuration files not found");
        }
    }
    
    private Map<String, Object> loadConfigFile(Yaml yaml, String filename) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
        if (inputStream != null) {
            logger.info("Loading configuration from: {}", filename);
            return yaml.load(inputStream);
        } else {
            logger.debug("Configuration file {} not found", filename);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeConfigs(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new java.util.HashMap<>(base);
        
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            String key = entry.getKey();
            Object overrideValue = entry.getValue();
            
            if (overrideValue instanceof Map && result.get(key) instanceof Map) {
                // Recursively merge nested maps
                result.put(key, mergeConfigs((Map<String, Object>) result.get(key), (Map<String, Object>) overrideValue));
            } else {
                // Override the value
                result.put(key, overrideValue);
            }
        }
        
        return result;
    }
    
    public String getString(String key) {
        return getString(key, null);
    }
    
    public String getString(String key, String defaultValue) {
        Object value = getNestedValue(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    public int getInt(String key) {
        return getInt(key, 0);
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = getNestedValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Cannot parse {} as integer, using default: {}", value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = getNestedValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private Object getNestedValue(String key) {
        String[] keys = key.split("\\.");
        Object current = config;
        
        for (String k : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(k);
            } else {
                return null;
            }
        }
        return current;
    }
}