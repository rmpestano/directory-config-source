/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.rmpestano.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author rafael-pestano
 *
 * Reads properties from file configured in DirectoryConfigSource.filePath system/env property or /data/application.properties if not specified
 * Any change to the file triggers the properties to be reloaded
 */
public class DirectoryConfigSource implements ConfigSource {

    private static final Logger LOG = Logger.getLogger(DirectoryConfigSource.class.getName());
    private Map<String, String> configPropertiesMap;
    private final WatchService watchService;

    public DirectoryConfigSource() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    private void initProperties() {
    	String configFilePath = resolveConfigFilePath();
        LOG.info("Initializing DirectoryConfigSource using directory: "+configFilePath);
        if(!new File(configFilePath).exists()) {
            throw new RuntimeException(String.format("Directory Config source not initialized becase directory %s does not exists", configFilePath));
        }
        
        configPropertiesMap = new HashMap<>();
        Path configDirectory = Paths.get(new File(configFilePath).getParentFile().getPath());//directory to watch for changes
        try {
            configDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            ManagedExecutorService executorService = (ManagedExecutorService) InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
            executorService.submit(() -> {
                WatchKey key;
                try {
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            LOG.log(Level.INFO, "Config change detected, reloading properties from: {0}", event.context());
                            loadProperties(configFilePath);
                        }
                        key.reset();
                    }
                } catch (InterruptedException | RuntimeException e) {
                    LOG.log(Level.SEVERE,"Problem to initialize properties.", e);
                }
            });
        } catch (IOException | NamingException e1) {
            LOG.log(Level.SEVERE,"Could not initialize DirectoryConfigSource.", e1);
        }
        loadProperties(configFilePath);
        LOG.log(Level.INFO,"DirectoryConfigSource source initialized.");
    }

    private void loadProperties(String configFilePath) throws RuntimeException {
        LOG.log(Level.INFO,"Loading properties...");
        try (final InputStream inputStream = new FileInputStream(new File(configFilePath))) {
            Properties properties = new Properties();
            properties.load(inputStream);
            properties.entrySet().forEach(p -> configPropertiesMap.put(p.getKey().toString(), p.getValue().toString()));
            LOG.log(Level.INFO,"Properties loaded.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not load configuration properties", e);
        }
    }

    @Override
    public int getOrdinal() {
        return 900;
    }

    @Override
    public Map<String, String> getProperties() {
        if (configPropertiesMap == null) {
            initProperties(); //lazy initialization so we can access managed executor service via JNDI.
        }
        return configPropertiesMap;
    }

    @Override
    public String getValue(String key) {
        if (getProperties().containsKey(key)) {
            return getProperties().get(key);
        }
        return null;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    private String resolveConfigFilePath() {
        String configFilePath = System.getProperty("DirectoryConfigSource.filePath", System.getenv("DirectoryConfigSource.filePath"));
        if(configFilePath == null) {
            configFilePath = "/data/application.properties";
        }
        return configFilePath;
    }

}
