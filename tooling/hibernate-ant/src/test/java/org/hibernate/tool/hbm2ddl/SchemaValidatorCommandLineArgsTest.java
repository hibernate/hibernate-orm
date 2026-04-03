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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests SchemaValidator.CommandLineArgs via reflection since it is a private inner class.
 */
public class SchemaValidatorCommandLineArgsTest {

	@Test
	public void testParseEmpty() throws Exception {
		Object args = parseArgs(new String[]{});
		assertNull(getField(args, "propertiesFile"));
		assertNull(getField(args, "cfgXmlFile"));
		assertNull(getField(args, "implicitNamingStrategy"));
		assertNull(getField(args, "physicalNamingStrategy"));
		assertTrue(((java.util.List<?>) getField(args, "hbmXmlFiles")).isEmpty());
		assertTrue(((java.util.List<?>) getField(args, "jarFiles")).isEmpty());
	}

	@Test
	public void testParseProperties() throws Exception {
		Object args = parseArgs(new String[]{"--properties=/tmp/hibernate.properties"});
		assertEquals("/tmp/hibernate.properties", getField(args, "propertiesFile"));
	}

	@Test
	public void testParseConfig() throws Exception {
		Object args = parseArgs(new String[]{"--config=/tmp/hibernate.cfg.xml"});
		assertEquals("/tmp/hibernate.cfg.xml", getField(args, "cfgXmlFile"));
	}

	@Test
	public void testParseImplicitNaming() throws Exception {
		Object args = parseArgs(new String[]{"--implicit-naming=org.example.MyStrategy"});
		assertEquals("org.example.MyStrategy", getField(args, "implicitNamingStrategy"));
	}

	@Test
	public void testParsePhysicalNaming() throws Exception {
		Object args = parseArgs(new String[]{"--physical-naming=org.example.MyStrategy"});
		assertEquals("org.example.MyStrategy", getField(args, "physicalNamingStrategy"));
	}

	@Test
	public void testParseNamingDeprecated() throws Exception {
		// --naming= is deprecated, should not set any field but also not throw
		Object args = parseArgs(new String[]{"--naming=org.example.OldStrategy"});
		assertNull(getField(args, "implicitNamingStrategy"));
		assertNull(getField(args, "physicalNamingStrategy"));
	}

	@Test
	public void testParseJarFile() throws Exception {
		Object args = parseArgs(new String[]{"model.jar"});
		java.util.List<?> jarFiles = (java.util.List<?>) getField(args, "jarFiles");
		assertEquals(1, jarFiles.size());
		assertEquals("model.jar", jarFiles.get(0));
	}

	@Test
	public void testParseHbmFile() throws Exception {
		Object args = parseArgs(new String[]{"Person.hbm.xml"});
		java.util.List<?> hbmFiles = (java.util.List<?>) getField(args, "hbmXmlFiles");
		assertEquals(1, hbmFiles.size());
		assertEquals("Person.hbm.xml", hbmFiles.get(0));
	}

	@Test
	public void testParseMixed() throws Exception {
		Object args = parseArgs(new String[]{
				"--properties=/tmp/test.properties",
				"--config=/tmp/hibernate.cfg.xml",
				"Person.hbm.xml",
				"model.jar",
				"--implicit-naming=org.example.ImplicitStrategy"
		});
		assertEquals("/tmp/test.properties", getField(args, "propertiesFile"));
		assertEquals("/tmp/hibernate.cfg.xml", getField(args, "cfgXmlFile"));
		assertEquals("org.example.ImplicitStrategy", getField(args, "implicitNamingStrategy"));
		java.util.List<?> hbmFiles = (java.util.List<?>) getField(args, "hbmXmlFiles");
		assertEquals(1, hbmFiles.size());
		java.util.List<?> jarFiles = (java.util.List<?>) getField(args, "jarFiles");
		assertEquals(1, jarFiles.size());
	}

	private Object parseArgs(String[] args) throws Exception {
		Class<?> cmdLineArgsClass = null;
		for (Class<?> inner : SchemaValidator.class.getDeclaredClasses()) {
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
		java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(obj);
	}
}
