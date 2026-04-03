/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class RevengSpecTest {

	@Test
	void testRevengSpec() {
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		for(Field field : RevengSpec.class.getDeclaredFields()) {
			fieldMap.put(field.getName(), field);
		}
		assertNotNull(fieldMap.get("sqlToRun"));
		assertNotNull(fieldMap.get("hibernateProperties"));
		assertNotNull(fieldMap.get("outputFolder"));
		assertNotNull(fieldMap.get("packageName"));
		assertNotNull(fieldMap.get("revengStrategy"));
		assertNotNull(fieldMap.get("revengFile"));
		assertNotNull(fieldMap.get("generateAnnotations"));
		assertNotNull(fieldMap.get("useGenerics"));
		assertNotNull(fieldMap.get("templatePath"));
	}

	@Test
	void testRevengSpecDefaults() {
		RevengSpec spec = new RevengSpec();
		assertEquals("", spec.sqlToRun);
		assertEquals("hibernate.properties", spec.hibernateProperties);
		assertEquals("generated-sources", spec.outputFolder);
		assertEquals("", spec.packageName);
		assertNull(spec.revengStrategy);
		assertNull(spec.revengFile);
		assertTrue(spec.generateAnnotations);
		assertTrue(spec.useGenerics);
		assertNull(spec.templatePath);
	}

	@Test
	void testRevengSpecSetValues() {
		RevengSpec spec = new RevengSpec();
		spec.sqlToRun = "CREATE TABLE test (id INT)";
		spec.hibernateProperties = "custom.properties";
		spec.outputFolder = "src/gen";
		spec.packageName = "com.example";
		spec.revengStrategy = "com.example.MyStrategy";
		spec.revengFile = "reveng.xml";
		spec.generateAnnotations = false;
		spec.useGenerics = false;
		spec.templatePath = "/templates";

		assertEquals("CREATE TABLE test (id INT)", spec.sqlToRun);
		assertEquals("custom.properties", spec.hibernateProperties);
		assertEquals("src/gen", spec.outputFolder);
		assertEquals("com.example", spec.packageName);
		assertEquals("com.example.MyStrategy", spec.revengStrategy);
		assertEquals("reveng.xml", spec.revengFile);
		assertFalse(spec.generateAnnotations);
		assertFalse(spec.useGenerics);
		assertEquals("/templates", spec.templatePath);
	}

}
