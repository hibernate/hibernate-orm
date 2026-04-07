/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the SchemaUpdate.CommandLineArgs parsing to cover
 * various flags and branch combinations.
 */
public class SchemaUpdateCommandLineTest {

	private Object parseArgs(String[] args) throws Exception {
		Class<?> cmdLineArgsClass = null;
		for (Class<?> inner : SchemaUpdate.class.getDeclaredClasses()) {
			if (inner.getSimpleName().equals("CommandLineArgs")) {
				cmdLineArgsClass = inner;
				break;
			}
		}
		assertNotNull(cmdLineArgsClass, "CommandLineArgs inner class not found");
		Method parseMethod = cmdLineArgsClass.getDeclaredMethod("parseCommandLineArgs", String[].class);
		parseMethod.setAccessible(true);
		return parseMethod.invoke(null, (Object) args);
	}

	private Object getField(Object obj, String fieldName) throws Exception {
		var field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(obj);
	}

	@Test
	public void testParseDefaults() throws Exception {
		Object args = parseArgs(new String[]{"test.hbm.xml"});
		assertNull(getField(args, "propertiesFile"));
		assertNull(getField(args, "cfgXmlFile"));
		assertNull(getField(args, "outputFile"));
		assertNull(getField(args, "delimiter"));
		assertNull(getField(args, "implicitNamingStrategyImplName"));
		assertNull(getField(args, "physicalNamingStrategyImplName"));
	}

	@Test
	public void testParsePropertiesFlag() throws Exception {
		Object args = parseArgs(new String[]{"--properties=hibernate.properties", "test.hbm.xml"});
		assertEquals("hibernate.properties", getField(args, "propertiesFile"));
	}

	@Test
	public void testParseConfigFlag() throws Exception {
		Object args = parseArgs(new String[]{"--config=hibernate.cfg.xml", "test.hbm.xml"});
		assertEquals("hibernate.cfg.xml", getField(args, "cfgXmlFile"));
	}

	@Test
	public void testParseOutputFlag() throws Exception {
		Object args = parseArgs(new String[]{"--output=update.sql", "test.hbm.xml"});
		assertEquals("update.sql", getField(args, "outputFile"));
	}

	@Test
	public void testParseDelimiterFlag() throws Exception {
		Object args = parseArgs(new String[]{"--delimiter=;", "test.hbm.xml"});
		assertEquals(";", getField(args, "delimiter"));
	}

	@Test
	public void testParseImplicitNamingFlag() throws Exception {
		Object args = parseArgs(new String[]{"--implicit-naming=com.example.My", "test.hbm.xml"});
		assertEquals("com.example.My", getField(args, "implicitNamingStrategyImplName"));
	}

	@Test
	public void testParsePhysicalNamingFlag() throws Exception {
		Object args = parseArgs(new String[]{"--physical-naming=com.example.My", "test.hbm.xml"});
		assertEquals("com.example.My", getField(args, "physicalNamingStrategyImplName"));
	}

	@Test
	public void testParseNamingDeprecated() throws Exception {
		Object args = parseArgs(new String[]{"--naming=com.example.Old", "test.hbm.xml"});
		assertNotNull(args);
	}

	@Test
	public void testParseQuietFlag() throws Exception {
		Object args = parseArgs(new String[]{"--quiet", "test.hbm.xml"});
		assertNotNull(args);
	}

	@Test
	public void testParseTextFlag() throws Exception {
		Object args = parseArgs(new String[]{"--text", "test.hbm.xml"});
		assertNotNull(args);
	}

	@Test
	public void testParseTargetFlag() throws Exception {
		Object args = parseArgs(new String[]{"--target=database", "test.hbm.xml"});
		assertNotNull(getField(args, "targetTypes"));
	}

	@Test
	public void testParseTextAndTargetWarning() throws Exception {
		Object args = parseArgs(new String[]{"--text", "--target=database", "test.hbm.xml"});
		assertNotNull(getField(args, "targetTypes"));
	}

	@Test
	public void testParseJarFile() throws Exception {
		Object args = parseArgs(new String[]{"entities.jar"});
		@SuppressWarnings("unchecked")
		java.util.List<String> jarFiles = (java.util.List<String>) getField(args, "jarFiles");
		assertEquals(1, jarFiles.size());
	}

	@Test
	public void testParseMixedFiles() throws Exception {
		Object args = parseArgs(new String[]{"Person.hbm.xml", "entities.jar"});
		@SuppressWarnings("unchecked")
		java.util.List<String> hbmFiles = (java.util.List<String>) getField(args, "hbmXmlFiles");
		@SuppressWarnings("unchecked")
		java.util.List<String> jarFiles = (java.util.List<String>) getField(args, "jarFiles");
		assertEquals(1, hbmFiles.size());
		assertEquals(1, jarFiles.size());
	}
}
