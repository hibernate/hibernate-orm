/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the CommandLineArgs parsing in SchemaExport to cover
 * various flags and branch combinations.
 */
public class SchemaExportCommandLineTest {

	private Object parseArgs(String[] args) throws Exception {
		Class<?> cmdLineArgsClass = Class.forName(SchemaExport.class.getName() + "$CommandLineArgs");
		Method parseMethod = cmdLineArgsClass.getDeclaredMethod("parseCommandLineArgs", String[].class);
		parseMethod.setAccessible(true);
		return parseMethod.invoke(null, (Object) args);
	}

	private Object getField(Object obj, String fieldName) throws Exception {
		java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(obj);
	}

	private void assertJarRejected(String... args) {
		final InvocationTargetException exception = assertThrows(
				InvocationTargetException.class,
				() -> parseArgs(args)
		);
		assertTrue(exception.getCause() instanceof IllegalArgumentException);
		assertTrue(exception.getCause().getMessage().contains("Jar file mapping discovery is no longer supported"));
	}

	@Test
	public void testParseImportFlag() throws Exception {
		Object args = parseArgs(new String[]{"--import=import.sql", "test.mapping.xml"});
		assertEquals("import.sql", getField(args, "importFile"));
	}

	@Test
	public void testParseConfigFlag() throws Exception {
		Object args = parseArgs(new String[]{"--config=hibernate.cfg.xml", "test.mapping.xml"});
		assertEquals("hibernate.cfg.xml", getField(args, "cfgXmlFile"));
	}

	@Test
	public void testParseImplicitNamingFlag() throws Exception {
		Object args = parseArgs(new String[]{"--implicit-naming=com.example.MyStrategy", "test.mapping.xml"});
		assertEquals("com.example.MyStrategy", getField(args, "implicitNamingStrategyImplName"));
	}

	@Test
	public void testParsePhysicalNamingFlag() throws Exception {
		Object args = parseArgs(new String[]{"--physical-naming=com.example.MyPhysical", "test.mapping.xml"});
		assertEquals("com.example.MyPhysical", getField(args, "physicalNamingStrategyImplName"));
	}

	@Test
	public void testParseNamingDeprecatedFlag() throws Exception {
		// --naming= should trigger deprecation warning but not fail
		Object args = parseArgs(new String[]{"--naming=com.example.Old", "test.mapping.xml"});
		assertNotNull(args);
	}

	@Test
	public void testParseCreateFlag() throws Exception {
		Object args = parseArgs(new String[]{"--create", "test.mapping.xml"});
		// --create without --action= uses Action.interpret(drop=false, create=true) -> CREATE
		assertEquals(SchemaExport.Action.CREATE, getField(args, "action"));
	}

	@Test
	public void testParseQuietFlag() throws Exception {
		Object args = parseArgs(new String[]{"--quiet", "test.mapping.xml"});
		// --quiet disables script output
		assertNotNull(args);
	}

	@Test
	public void testParseJarFile() throws Exception {
		assertJarRejected("entities.jar");
	}

	@Test
	public void testParseMappingAndJarMixed() throws Exception {
		assertJarRejected("Person.mapping.xml", "entities.jar", "Address.mapping.xml");
	}

	@Test
	public void testParseDropAndCreateWithActionWarning() throws Exception {
		// Both --drop and --action= specified: triggers warning
		Object args = parseArgs(new String[]{"--drop", "--action=create", "test.mapping.xml"});
		// --action takes precedence
		assertEquals(SchemaExport.Action.CREATE, getField(args, "action"));
	}

	@Test
	public void testParseTextAndTargetWarning() throws Exception {
		// Both --text and --target= specified: triggers warning
		Object args = parseArgs(new String[]{"--text", "--target=database", "test.mapping.xml"});
		assertNotNull(getField(args, "targetTypes"));
	}

	@Test
	public void testParseDefaults() throws Exception {
		Object args = parseArgs(new String[]{"test.mapping.xml"});
		assertFalse((boolean) getField(args, "halt"));
		assertFalse((boolean) getField(args, "format"));
		assertFalse((boolean) getField(args, "manageNamespaces"));
		assertNull(getField(args, "delimiter"));
		assertNull(getField(args, "outputFile"));
		assertNull(getField(args, "propertiesFile"));
		assertNull(getField(args, "cfgXmlFile"));
		assertNull(getField(args, "implicitNamingStrategyImplName"));
		assertNull(getField(args, "physicalNamingStrategyImplName"));
	}

	@Test
	public void testSchemaExportSetters() {
		SchemaExport export = new SchemaExport();
		export.setOutputFile("output.sql");
		assertEquals("output.sql", export.outputFile);

		export.setOverrideOutputFileContent();
		assertFalse(export.append);

		export.setImportFiles("import1.sql,import2.sql");

		export.setDelimiter(";");
		assertEquals(";", export.delimiter);

		export.setFormat(true);
		assertTrue(export.format);

		export.setHaltOnError(true);
		assertTrue(export.haltOnError);

		export.setManageNamespaces(true);
		assertTrue(export.manageNamespaces);
	}

	@Test
	public void testSchemaExportTypeEnum() {
		assertTrue(SchemaExport.Type.CREATE.doCreate());
		assertFalse(SchemaExport.Type.CREATE.doDrop());

		assertFalse(SchemaExport.Type.DROP.doCreate());
		assertTrue(SchemaExport.Type.DROP.doDrop());

		assertTrue(SchemaExport.Type.BOTH.doCreate());
		assertTrue(SchemaExport.Type.BOTH.doDrop());

		assertFalse(SchemaExport.Type.NONE.doCreate());
		assertFalse(SchemaExport.Type.NONE.doDrop());
	}

	@Test
	public void testActionInterpret() throws Exception {
		// Access private interpret method via reflection
		Method interpret = SchemaExport.Action.class.getDeclaredMethod("interpret", boolean.class, boolean.class);
		interpret.setAccessible(true);

		assertEquals(SchemaExport.Action.DROP, interpret.invoke(null, true, false));
		assertEquals(SchemaExport.Action.CREATE, interpret.invoke(null, false, true));
		assertEquals(SchemaExport.Action.BOTH, interpret.invoke(null, false, false));
	}
}
