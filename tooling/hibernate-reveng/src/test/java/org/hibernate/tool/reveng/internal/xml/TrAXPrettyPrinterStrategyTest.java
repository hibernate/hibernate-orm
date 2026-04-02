/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrAXPrettyPrinterStrategyTest {

	@Test
	public void testPrettyPrintSimpleXml() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		strategy.setOmitXmlDeclaration(true);
		String input = "<root><child>text</child></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("<root>"));
		assertTrue(result.contains("<child>text</child>"));
		assertTrue(result.contains("</root>"));
	}

	@Test
	public void testPrettyPrintWithAttributes() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		strategy.setOmitXmlDeclaration(true);
		String input = "<root attr=\"value\"><child/></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("attr=\"value\""));
	}

	@Test
	public void testIndentGetterSetter() {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		assertEquals(4, strategy.getIndent());
		strategy.setIndent(2);
		assertEquals(2, strategy.getIndent());
	}

	@Test
	public void testOmitXmlDeclarationGetterSetter() {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		assertFalse(strategy.isOmitXmlDeclaration());
		strategy.setOmitXmlDeclaration(true);
		assertTrue(strategy.isOmitXmlDeclaration());
	}

	@Test
	public void testPrettyPrintWithXmlDeclaration() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		strategy.setOmitXmlDeclaration(false);
		String input = "<root><child/></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("<?xml"));
	}

	@Test
	public void testPrettyPrintWithoutXmlDeclaration() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		strategy.setOmitXmlDeclaration(true);
		String input = "<root><child/></root>";
		String result = strategy.prettyPrint(input);
		assertFalse(result.contains("<?xml"));
	}
}
