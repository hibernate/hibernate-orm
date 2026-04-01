/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Cfg2HbmToolTest {

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesNull() {
		assertNull(Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(null, new Properties()));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesEmpty() {
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(new Properties(), new Properties());
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesFiltersTarget() {
		Properties props = new Properties();
		props.put("target_table", "some_table");
		props.put("sequence_name", "my_seq");
		Properties env = new Properties();
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("target_table"));
		assertTrue(result.containsKey("sequence_name"));
		assertEquals("my_seq", result.getProperty("sequence_name"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesFiltersDefaultSchema() {
		Properties props = new Properties();
		props.put("schema", "public");
		Properties env = new Properties();
		env.put("hibernate.default_schema", "public");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("schema"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesKeepsNonDefaultSchema() {
		Properties props = new Properties();
		props.put("schema", "custom");
		Properties env = new Properties();
		env.put("hibernate.default_schema", "public");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertTrue(result.containsKey("schema"));
		assertEquals("custom", result.getProperty("schema"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesFiltersDefaultCatalog() {
		Properties props = new Properties();
		props.put("catalog", "defaultCat");
		Properties env = new Properties();
		env.put("hibernate.default_catalog", "defaultCat");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("catalog"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesKeepsNonDefaultCatalog() {
		Properties props = new Properties();
		props.put("catalog", "customCat");
		Properties env = new Properties();
		env.put("hibernate.default_catalog", "defaultCat");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertTrue(result.containsKey("catalog"));
		assertEquals("customCat", result.getProperty("catalog"));
	}

	@Test
	public void testGetFilteredIdentifierGeneratorPropertiesMixedKeys() {
		Properties props = new Properties();
		props.put("target_column", "id");
		props.put("schema", "public");
		props.put("catalog", "myCat");
		props.put("sequence_name", "seq1");
		props.put("initial_value", "1");
		Properties env = new Properties();
		env.put("hibernate.default_schema", "public");
		Properties result = Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(props, env);
		assertFalse(result.containsKey("target_column"));
		assertFalse(result.containsKey("schema"));
		assertTrue(result.containsKey("catalog"));
		assertTrue(result.containsKey("sequence_name"));
		assertTrue(result.containsKey("initial_value"));
	}
}
