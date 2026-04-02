/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DOM3LSPrettyPrinterStrategyTest {

	@Test
	public void testPrettyPrintSimpleXml() throws Exception {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		String input = "<root><child>text</child></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("<root>"));
		assertTrue(result.contains("<child>text</child>"));
		assertTrue(result.contains("</root>"));
	}

	@Test
	public void testPrettyPrintWithAttributes() throws Exception {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		String input = "<root attr=\"value\"><child/></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("attr=\"value\""));
	}

	@Test
	public void testOutputCommentsGetterSetter() {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		assertFalse(strategy.isOutputComments());
		strategy.setOutputComments(true);
		assertTrue(strategy.isOutputComments());
	}

	@Test
	public void testPrettyPrintWithComments() throws Exception {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		strategy.setOutputComments(true);
		String input = "<root><!-- a comment --><child/></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("a comment"));
	}

	@Test
	public void testPrettyPrintNestedElements() throws Exception {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		String input = "<root><parent><child><grandchild/></child></parent></root>";
		String result = strategy.prettyPrint(input);
		assertTrue(result.contains("<grandchild/>") || result.contains("<grandchild></grandchild>"));
	}
}
