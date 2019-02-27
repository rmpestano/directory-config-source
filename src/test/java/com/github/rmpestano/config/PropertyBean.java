package com.github.rmpestano.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Field;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * This bean is needed because there is no standard way to pass system
 * properties to arquillian containers And our DirectoryConfigSource needs a
 * system property which has the path to the config file
 * 
 * @author rafael-pestano
 *
 */
@Startup
@Singleton
public class PropertyBean implements Serializable {

	@Inject
	DirectoryConfigSource directoryConfigSource;

	@PostConstruct 
	public void initDirectoryConfigSourceProperty() {
		try {
			Field configPropertiesMapField = directoryConfigSource.getClass().getDeclaredField("configPropertiesMap");
			configPropertiesMapField.setAccessible(true);
			configPropertiesMapField.set(directoryConfigSource, null); // force re-initialization of configMap because the test runs after Config source initialization (before bean discovery)
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream("/META-INF/file")))) {
				String property = bufferedReader.readLine();
				System.setProperty("DirectoryConfigSource.filePath", property);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
