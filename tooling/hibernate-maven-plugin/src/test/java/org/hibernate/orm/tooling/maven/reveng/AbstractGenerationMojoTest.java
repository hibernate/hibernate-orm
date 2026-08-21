/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AbstractGenerationMojoTest {

	@TempDir
	private File tempDir;

	/**
	 * Concrete subclass for testing the abstract class.
	 */
	static class TestMojo extends AbstractGenerationMojo {
		MetadataDescriptor lastDescriptor;

		@Override
		protected void executeExporter(MetadataDescriptor metadataDescriptor) throws MojoFailureException {
			lastDescriptor = metadataDescriptor;
		}
	}

	private void setField(Object obj, String fieldName, Object value) throws Exception {
		Class<?> clazz = obj.getClass();
		while (clazz != null) {
			try {
				Field f = clazz.getDeclaredField(fieldName);
				f.setAccessible(true);
				f.set(obj, value);
				return;
			}
			catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}

	@Test
	public void testLoadPropertiesFileValid() throws Exception {
		TestMojo mojo = new TestMojo();
		File propsFile = new File(tempDir, "hibernate.properties");
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		props.setProperty("hibernate.connection.url", "jdbc:h2:mem:test");
		try (FileOutputStream fos = new FileOutputStream(propsFile)) {
			props.store(fos, null);
		}
		setField(mojo, "propertyFile", propsFile);
		Method loadPropertiesFile = AbstractGenerationMojo.class.getDeclaredMethod("loadPropertiesFile");
		loadPropertiesFile.setAccessible(true);
		Properties loaded = (Properties) loadPropertiesFile.invoke(mojo);
		assertNotNull(loaded);
		assertEquals("org.hibernate.dialect.H2Dialect", loaded.getProperty("hibernate.dialect"));
	}

	@Test
	public void testLoadPropertiesFileNotFound() throws Exception {
		TestMojo mojo = new TestMojo();
		setField(mojo, "propertyFile", new File("/nonexistent/hibernate.properties"));
		Method loadPropertiesFile = AbstractGenerationMojo.class.getDeclaredMethod("loadPropertiesFile");
		loadPropertiesFile.setAccessible(true);
		try {
			loadPropertiesFile.invoke(mojo);
		}
		catch (java.lang.reflect.InvocationTargetException e) {
			assertNotNull(e.getCause());
			assertEquals(MojoFailureException.class, e.getCause().getClass());
		}
	}

	@Test
	public void testSetupReverseEngineeringStrategy() throws Exception {
		TestMojo mojo = new TestMojo();
		// Set fields to exercise the method
		setField(mojo, "packageName", "com.example.model");
		setField(mojo, "revengFile", null);
		setField(mojo, "revengStrategy", null);
		setField(mojo, "detectManyToMany", true);
		setField(mojo, "detectOneToOne", true);
		setField(mojo, "detectOptimisticLock", true);
		setField(mojo, "createCollectionForForeignKey", true);
		setField(mojo, "createManyToOneForForeignKey", true);
		Method setupStrategy = AbstractGenerationMojo.class.getDeclaredMethod("setupReverseEngineeringStrategy");
		setupStrategy.setAccessible(true);
		Object strategy = setupStrategy.invoke(mojo);
		assertNotNull(strategy);
	}

	@Test
	public void testSetupReverseEngineeringStrategyWithRevengFile() throws Exception {
		TestMojo mojo = new TestMojo();
		File revengFile = new File(tempDir, "hibernate.reveng.xml");
		// Create a minimal reveng file
		try (FileOutputStream fos = new FileOutputStream(revengFile)) {
			fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<hibernate-reverse-engineering/>".getBytes());
		}
		setField(mojo, "packageName", "com.example");
		setField(mojo, "revengFile", revengFile);
		setField(mojo, "revengStrategy", null);
		setField(mojo, "detectManyToMany", false);
		setField(mojo, "detectOneToOne", false);
		setField(mojo, "detectOptimisticLock", false);
		setField(mojo, "createCollectionForForeignKey", false);
		setField(mojo, "createManyToOneForForeignKey", false);
		Method setupStrategy = AbstractGenerationMojo.class.getDeclaredMethod("setupReverseEngineeringStrategy");
		setupStrategy.setAccessible(true);
		Object strategy = setupStrategy.invoke(mojo);
		assertNotNull(strategy);
	}

	@Test
	public void testCreateJdbcDescriptor() throws Exception {
		TestMojo mojo = new TestMojo();
		setField(mojo, "packageName", "com.example");
		setField(mojo, "revengFile", null);
		setField(mojo, "revengStrategy", null);
		setField(mojo, "detectManyToMany", true);
		setField(mojo, "detectOneToOne", true);
		setField(mojo, "detectOptimisticLock", true);
		setField(mojo, "createCollectionForForeignKey", true);
		setField(mojo, "createManyToOneForForeignKey", true);

		// First get a strategy
		Method setupStrategy = AbstractGenerationMojo.class.getDeclaredMethod("setupReverseEngineeringStrategy");
		setupStrategy.setAccessible(true);
		Object strategy = setupStrategy.invoke(mojo);

		// Then create descriptor
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		Method createJdbcDescriptor = AbstractGenerationMojo.class.getDeclaredMethod(
				"createJdbcDescriptor",
				org.hibernate.tool.reveng.api.core.RevengStrategy.class,
				Properties.class);
		createJdbcDescriptor.setAccessible(true);
		Object descriptor = createJdbcDescriptor.invoke(mojo, strategy, props);
		assertNotNull(descriptor);
	}
}
