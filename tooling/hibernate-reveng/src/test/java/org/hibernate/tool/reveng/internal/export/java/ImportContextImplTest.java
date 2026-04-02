/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportContextImplTest {

	@Test
	public void testImportSimpleClass() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("org.hibernate.Session");
		assertEquals("Session", result);
	}

	@Test
	public void testImportSamePackageClass() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("com.example.MyEntity");
		assertEquals("MyEntity", result);
	}

	@Test
	public void testImportJavaLangClass() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("java.lang.String");
		assertEquals("String", result);
	}

	@Test
	public void testImportPrimitive() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("int");
		assertEquals("int", result);
	}

	@Test
	public void testImportWithGenerics() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("java.util.Collection<org.hibernate.Session>");
		assertEquals("Collection<org.hibernate.Session>", result);
	}

	@Test
	public void testImportWithArray() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("org.hibernate.Session[]");
		assertEquals("Session[]", result);
	}

	@Test
	public void testImportInnerClass() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.importType("org.hibernate.Session$Inner");
		assertEquals("Session.Inner", result);
	}

	@Test
	public void testImportConflictingSimpleNames() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String first = ctx.importType("org.a.Foo");
		assertEquals("Foo", first);
		// Same simple name, different package — must use FQN
		String second = ctx.importType("org.b.Foo");
		assertEquals("org.b.Foo", second);
	}

	@Test
	public void testGenerateImportsExcludesSamePackage() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("com.example.MyEntity");
		String imports = ctx.generateImports();
		assertFalse(imports.contains("com.example.MyEntity"));
	}

	@Test
	public void testGenerateImportsExcludesJavaLang() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("java.lang.String");
		String imports = ctx.generateImports();
		assertFalse(imports.contains("java.lang.String"));
	}

	@Test
	public void testGenerateImportsExcludesPrimitives() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("int");
		String imports = ctx.generateImports();
		assertFalse(imports.contains("int"));
	}

	@Test
	public void testGenerateImportsIncludesExternalClass() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		ctx.importType("org.hibernate.Session");
		String imports = ctx.generateImports();
		assertTrue(imports.contains("import org.hibernate.Session;"));
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
	public void testStaticImportWildcard() {
		ImportContextImpl ctx = new ImportContextImpl("com.example");
		String result = ctx.staticImport("org.junit.Assert", "*");
		assertEquals("", result);
	}

	@Test
	public void testImportDefaultPackageClass() {
		ImportContextImpl ctx = new ImportContextImpl("");
		ctx.importType("MyClass");
		String imports = ctx.generateImports();
		assertFalse(imports.contains("MyClass"));
	}
}
