/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoopImportContextTest {

	@Test
	public void testImportTypeReturnsUnchanged() {
		NoopImportContext ctx = new NoopImportContext();
		assertEquals("java.lang.String", ctx.importType("java.lang.String"));
		assertEquals("int", ctx.importType("int"));
		assertEquals("com.example.MyClass", ctx.importType("com.example.MyClass"));
	}

	@Test
	public void testStaticImportReturnsFqcn() {
		NoopImportContext ctx = new NoopImportContext();
		assertEquals("java.lang.Math", ctx.staticImport("java.lang.Math", "max"));
	}

	@Test
	public void testGenerateImportsReturnsEmpty() {
		NoopImportContext ctx = new NoopImportContext();
		assertEquals("", ctx.generateImports());
	}
}
