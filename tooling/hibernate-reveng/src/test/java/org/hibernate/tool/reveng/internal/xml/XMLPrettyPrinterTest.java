/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.hibernate.tool.reveng.api.xml.XMLPrettyPrinterStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XMLPrettyPrinterTest {

	@Test
	public void testFactoryReturnsDefaultStrategy() {
		XMLPrettyPrinterStrategy strategy = XMLPrettyPrinterStrategyFactory.newXMLPrettyPrinterStrategy();
		assertNotNull(strategy);
		assertInstanceOf(TrAXPrettyPrinterStrategy.class, strategy);
	}

	@Test
	public void testTrAXPrettyPrintSimpleXml() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child>text</child></root>";
		String result = strategy.prettyPrint(xml);
		assertNotNull(result);
		assertTrue(result.contains("<root>"));
		assertTrue(result.contains("<child>"));
		assertTrue(result.contains("text"));
	}

	@Test
	public void testTrAXIndentSettings() {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		strategy.setIndent(2);
		assertEquals(2, strategy.getIndent());
	}

	@Test
	public void testTrAXOmitXmlDeclaration() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		strategy.setOmitXmlDeclaration(true);
		assertTrue(strategy.isOmitXmlDeclaration());

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child/></root>";
		String result = strategy.prettyPrint(xml);
		assertNotNull(result);
	}

	@Test
	public void testDOM3LSPrettyPrint() throws Exception {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child>text</child></root>";
		String result = strategy.prettyPrint(xml);
		assertNotNull(result);
		assertTrue(result.contains("<root>"));
		assertTrue(result.contains("<child>"));
	}

	@Test
	public void testDOM3LSOutputComments() {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		strategy.setOutputComments(true);
		assertTrue(strategy.isOutputComments());
	}

	@Test
	public void testTrAXPrettyPrintWithNestedElements() throws Exception {
		TrAXPrettyPrinterStrategy strategy = new TrAXPrettyPrinterStrategy();
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<root><parent><child1>a</child1><child2>b</child2></parent></root>";
		String result = strategy.prettyPrint(xml);
		assertNotNull(result);
		assertTrue(result.contains("<parent>"));
		assertTrue(result.contains("<child1>"));
		assertTrue(result.contains("<child2>"));
	}

	@Test
	public void testDOM3LSPrettyPrintWithComments() throws Exception {
		DOM3LSPrettyPrinterStrategy strategy = new DOM3LSPrettyPrinterStrategy();
		strategy.setOutputComments(true);
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><!-- comment --><child/></root>";
		String result = strategy.prettyPrint(xml);
		assertNotNull(result);
		assertTrue(result.contains("<root>"));
	}

	private void assertEquals(int expected, int actual) {
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
	}
}
