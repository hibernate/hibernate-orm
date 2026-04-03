/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportContextImplTest {

	@Test
	public void testImportTypeSimple() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("List", ctx.importType("java.util.List"));
	}

	@Test
	public void testImportTypeSamePackage() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("Person", ctx.importType("com.example.Person"));
	}

	@Test
	public void testImportTypeJavaLang() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("String", ctx.importType("java.lang.String"));
	}

	@Test
	public void testImportTypePrimitive() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("int", ctx.importType("int"));
	}

	@Test
	public void testImportTypeWithGenerics() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("java.util.List<java.lang.String>");
		assertEquals("List<java.lang.String>", result);
	}

	@Test
	public void testImportTypeWithArray() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("java.util.List[]");
		assertEquals("List[]", result);
	}

	@Test
	public void testImportTypeInnerClass() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("com.other.Outer$Inner");
		assertEquals("Outer.Inner", result);
	}

	@Test
	public void testImportTypeConflict() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("List", ctx.importType("java.util.List"));
		assertEquals("com.other.List", ctx.importType("com.other.List"));
	}

	@Test
	public void testImportTypeSameTwice() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("List", ctx.importType("java.util.List"));
		assertEquals("List", ctx.importType("java.util.List"));
	}

	@Test
	public void testGenerateImportsEmpty() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		assertEquals("", ctx.generateImports());
	}

	@Test
	public void testGenerateImports() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("java.util.List");
		ctx.importType("java.util.Map");
		String imports = ctx.generateImports();
		assertTrue(imports.contains("import java.util.List;"));
		assertTrue(imports.contains("import java.util.Map;"));
	}

	@Test
	public void testGenerateImportsExcludesSamePackage() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("com.example.Person");
		assertEquals("", ctx.generateImports());
	}

	@Test
	public void testGenerateImportsExcludesJavaLang() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("java.lang.String");
		assertEquals("", ctx.generateImports());
	}

	@Test
	public void testGenerateImportsExcludesPrimitive() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("int");
		assertEquals("", ctx.generateImports());
	}

	@Test
	public void testStaticImport() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.staticImport("org.junit.Assert", "assertEquals");
		assertEquals("assertEquals", result);
		String imports = ctx.generateImports();
		assertTrue(imports.contains("import static org.junit.Assert.assertEquals;"));
	}

	@Test
	public void testStaticImportStar() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.staticImport("org.junit.Assert", "*");
		assertEquals("", result);
	}
}
