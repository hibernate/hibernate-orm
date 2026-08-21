/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import org.hibernate.boot.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativeMetadataDescriptorTest {

	private static Properties h2Props() {
		Properties props = new Properties();
		props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		props.put("hibernate.connection.driver_class", "org.h2.Driver");
		props.put("hibernate.connection.url", "jdbc:h2:mem:native_md_test");
		props.put("hibernate.connection.username", "sa");
		props.put("hibernate.connection.password", "");
		props.put("hibernate.default_schema", "");
		props.put("hibernate.default_catalog", "");
		return props;
	}

	@Test
	public void testConstructorWithProperties() {
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(null, null, h2Props());
		Properties result = descriptor.getProperties();
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	@Test
	public void testConstructorWithNullProperties() {
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(null, null, null);
		Properties result = descriptor.getProperties();
		assertNotNull(result);
	}

	@Test
	public void testConstructorWithMappingFiles(@TempDir File tempDir) throws IOException {
		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (InputStream in = getClass().getResourceAsStream(
				"/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(
				null, new File[]{hbmFile}, h2Props());
		Properties result = descriptor.getProperties();
		assertNotNull(result);
	}

	@Test
	public void testGetPropertiesReturnsCopy() {
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(null, null, h2Props());
		Properties result1 = descriptor.getProperties();
		Properties result2 = descriptor.getProperties();
		result1.put("extra.key", "value");
		assertFalse(result2.containsKey("extra.key"));
	}

	@Test
	public void testCreateMetadata() {
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(null, null, h2Props());
		Metadata metadata = descriptor.createMetadata();
		assertNotNull(metadata);
	}

	@Test
	public void testConstructorWithCfgXmlFile(@TempDir File tempDir) throws IOException {
		File cfgFile = new File(tempDir, "hibernate.cfg.xml");
		try (FileWriter w = new FileWriter(cfgFile)) {
			w.write("<?xml version='1.0' encoding='utf-8'?>\n");
			w.write("<!DOCTYPE hibernate-configuration PUBLIC\n");
			w.write("  \"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n");
			w.write("  \"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">\n");
			w.write("<hibernate-configuration><session-factory/></hibernate-configuration>\n");
		}
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(cfgFile, null, h2Props());
		assertNotNull(descriptor.getProperties());
	}

	@Test
	public void testConstructorWithJarMappingFile(@TempDir File tempDir) throws IOException {
		File jarFile = new File(tempDir, "mappings.jar");
		try (JarOutputStream jos = new JarOutputStream(
				Files.newOutputStream(jarFile.toPath()))) {
			jos.putNextEntry(new ZipEntry("META-INF/"));
			jos.closeEntry();
		}
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(
				null, new File[]{jarFile}, h2Props());
		assertNotNull(descriptor.getProperties());
	}

	@Test
	public void testCreateMetadataWithMappingFiles(@TempDir File tempDir) throws IOException {
		File hbmFile = new File(tempDir, "HelloWorld.hbm.xml");
		try (InputStream in = getClass().getResourceAsStream(
				"/org/hibernate/tool/reveng/hbm2x/DdlExporterTest/HelloWorld.hbm.xml")) {
			Files.copy(in, hbmFile.toPath());
		}
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(
				null, new File[]{hbmFile}, h2Props());
		Metadata metadata = descriptor.createMetadata();
		assertNotNull(metadata);
		assertTrue(metadata.getEntityBindings().iterator().hasNext());
	}
}
