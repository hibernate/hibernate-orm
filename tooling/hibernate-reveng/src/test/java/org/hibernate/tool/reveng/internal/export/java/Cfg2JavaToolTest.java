/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Cfg2JavaToolTest {

	private final Cfg2JavaTool tool = new Cfg2JavaTool();

	@Test
	public void testUnqualify() {
		assertEquals("String", tool.unqualify("java.lang.String"));
		assertEquals("MyClass", tool.unqualify("com.example.MyClass"));
		assertEquals("Simple", tool.unqualify("Simple"));
	}

	@Test
	public void testToJavaDoc() {
		String result = tool.toJavaDoc("Hello world", 0);
		assertTrue(result.contains("* Hello world"));
	}

	@Test
	public void testToJavaDocMultiLine() {
		String result = tool.toJavaDoc("Line1\nLine2", 0);
		assertTrue(result.contains("* Line1"));
		assertTrue(result.contains("* Line2"));
	}

	@Test
	public void testToJavaDocNull() {
		String result = tool.toJavaDoc(null, 0);
		assertEquals("", result);
	}

	@Test
	public void testToJavaDocWithIndent() {
		String result = tool.toJavaDoc("Hello", 4);
		assertTrue(result.contains("* Hello"));
		assertTrue(result.length() > " * Hello".length());
	}

	@Test
	public void testIsPrimitive() {
		assertTrue(tool.isPrimitive("int"));
		assertTrue(tool.isPrimitive("long"));
		assertTrue(tool.isPrimitive("boolean"));
		assertTrue(tool.isPrimitive("char"));
		assertTrue(tool.isPrimitive("byte"));
		assertTrue(tool.isPrimitive("short"));
		assertTrue(tool.isPrimitive("float"));
		assertTrue(tool.isPrimitive("double"));
		assertFalse(tool.isPrimitive("String"));
		assertFalse(tool.isPrimitive("Integer"));
	}

	@Test
	public void testIsArray() {
		assertTrue(tool.isArray("byte[]"));
		assertTrue(tool.isArray("String[]"));
		assertFalse(tool.isArray("String"));
		assertFalse(tool.isArray(null));
	}

	@Test
	public void testKeyWordCheck() {
		assertEquals("class_", tool.keyWordCheck("class"));
		assertEquals("return_", tool.keyWordCheck("return"));
		assertEquals("public_", tool.keyWordCheck("public"));
		assertEquals("myField", tool.keyWordCheck("myField"));
		assertEquals("name", tool.keyWordCheck("name"));
	}

	@Test
	public void testSimplePluralize() {
		assertEquals("products", tool.simplePluralize("product"));
		assertEquals("categories", tool.simplePluralize("category"));
		assertEquals("classes", tool.simplePluralize("class"));
	}

	@Test
	public void testIsNonPrimitiveTypeName() {
		assertFalse(Cfg2JavaTool.isNonPrimitiveTypeName("int"));
		assertFalse(Cfg2JavaTool.isNonPrimitiveTypeName("nonexistent_type_xyz"));
	}
}
