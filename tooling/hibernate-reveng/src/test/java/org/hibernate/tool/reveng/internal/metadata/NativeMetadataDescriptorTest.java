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
import java.nio.file.Files;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativeMetadataDescriptorTest {

	private static final String ORM_XML = """
			<?xml version="1.0" encoding="UTF-8"?>
			<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2">
				<entity class="org.hibernate.tool.hbm2ddl.HelloWorld" metadata-complete="true" access="FIELD">
					<table name="HELLO_WORLD"/>
					<attributes>
						<id name="id"><column length="10"/></id>
						<basic name="hello"><column length="5"/></basic>
						<basic name="world"/>
					</attributes>
				</entity>
			</entity-mappings>
			""";

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
		File ormFile = new File(tempDir, "HelloWorld.orm.xml");
		Files.writeString(ormFile.toPath(), ORM_XML);
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(
				null, new File[]{ormFile}, h2Props());
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
		File ormFile = new File(tempDir, "HelloWorld.orm.xml");
		Files.writeString(ormFile.toPath(), ORM_XML);
		NativeMetadataDescriptor descriptor = new NativeMetadataDescriptor(
				null, new File[]{ormFile}, h2Props());
		Metadata metadata = descriptor.createMetadata();
		assertNotNull(metadata);
		assertTrue(metadata.getEntityBindings().iterator().hasNext());
	}
}
