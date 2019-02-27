/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.rmpestano.config;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Provider;

import org.arquillian.container.chameleon.api.ChameleonTarget;
import org.arquillian.container.chameleon.runner.ArquillianChameleon;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rafael-pestano
 */
@RunWith(ArquillianChameleon.class)
@ChameleonTarget(value = "${arquillian.container}")
public class DirectoryConfigSourceTest {

	@Inject
	@ConfigProperty(name = "config.example")
	private Provider<String> property;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = create(WebArchive.class).addClasses(DirectoryConfigSource.class, PropertyBean.class)
				.addAsWebInfResource(new File("src/main/resources/META-INF/beans.xml"))
				.addAsManifestResource(new File(
						"src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
						"services/org.eclipse.microprofile.config.spi.ConfigSource")
				.addAsManifestResource(new StringAsset(System.getProperty("DirectoryConfigSource.filePath")), "file");//sysprop is only available here, we'll read its value and set the property in PropertyBean test class//
		System.out.println(war.toString(true));
		return war;
	}
	

	@Test
	public void shouldInjectPropertyFromDirectory() {
		assertNotNull(property.get());
		assertEquals(property.get(), "example value");
	}

	@Test
	public void shouldGetPropertyUpdatedValue() throws InterruptedException {
		assertNotNull(property.get());
		assertEquals("example value", property.get());
		replaceConfigFileContent();
		Thread.sleep(500);
		assertEquals("example value modified", property.get());
	}


	private void replaceConfigFileContent() {
		String filePath = System.getProperty("DirectoryConfigSource.filePath");
		Path configFilePath = Paths.get(filePath);
		try (Stream<String> lines = Files.lines(configFilePath)) {
			List<String> replaced = lines.map(line -> line.replaceAll("value", "value modified"))
					.collect(Collectors.toList());
			Files.write(configFilePath, replaced);
		} catch (IOException e) {
			throw new RuntimeException("Could not replace content of file:"+configFilePath);
		}
	}

}
