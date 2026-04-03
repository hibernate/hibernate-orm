/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractXMLPrettyPrinterStrategyTest {

	/**
	 * Concrete subclass for testing.
	 */
	private static class TestableStrategy extends AbstractXMLPrettyPrinterStrategy {
		@Override
		public String prettyPrint(String xml) {
			return xml;
		}
	}

	private final TestableStrategy strategy = new TestableStrategy();

	@Test
	public void testNewDocumentParsesXml() throws Exception {
		String xml = "<root><child>text</child></root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		assertNotNull(doc);
		assertEquals("root", doc.getDocumentElement().getTagName());
		assertEquals(1, doc.getDocumentElement().getChildNodes().getLength());
	}

	@Test
	public void testNewDocumentWithAttributes() throws Exception {
		String xml = "<root attr=\"value\"><child id=\"1\"/></root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		assertEquals("value", doc.getDocumentElement().getAttribute("attr"));
	}

	@Test
	public void testNewDocumentWithMultipleChildren() throws Exception {
		String xml = "<root><a/><b/><c/></root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		assertEquals("root", doc.getDocumentElement().getTagName());
		NodeList children = doc.getDocumentElement().getChildNodes();
		assertEquals(3, children.getLength());
	}

	@Test
	public void testRemoveWhitespace() throws Exception {
		String xml = "<root>  \n  <child>text</child>  \n  </root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		// Before removal, whitespace text nodes exist alongside the child element
		assertTrue(doc.getDocumentElement().getChildNodes().getLength() > 1);

		strategy.removeWhitespace(doc);

		// After removal, only the child element remains
		int afterCount = doc.getDocumentElement().getChildNodes().getLength();
		assertEquals(1, afterCount);
		assertEquals("child", doc.getDocumentElement().getChildNodes().item(0).getNodeName());
	}

	@Test
	public void testRemoveWhitespacePreservesContent() throws Exception {
		String xml = "<root><child>  content with spaces  </child></root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		strategy.removeWhitespace(doc);

		assertEquals("  content with spaces  ",
				doc.getDocumentElement().getFirstChild().getTextContent());
	}

	@Test
	public void testRemoveWhitespaceNested() throws Exception {
		String xml = "<root>\n  <parent>\n    <child>text</child>\n  </parent>\n</root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		strategy.removeWhitespace(doc);

		assertEquals(1, doc.getDocumentElement().getChildNodes().getLength());
		assertEquals("parent", doc.getDocumentElement().getFirstChild().getNodeName());
		assertEquals(1, doc.getDocumentElement().getFirstChild().getChildNodes().getLength());
	}

	@Test
	public void testNewDocumentWithEncoding() throws Exception {
		String xml = "<root><item>Ünïcödë</item></root>";
		Document doc = strategy.newDocument(xml, "UTF-8");

		assertNotNull(doc);
		assertEquals("Ünïcödë", doc.getDocumentElement().getFirstChild().getTextContent());
	}
}
