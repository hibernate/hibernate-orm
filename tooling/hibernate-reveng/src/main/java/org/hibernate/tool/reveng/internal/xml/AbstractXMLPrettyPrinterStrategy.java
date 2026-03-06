/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.hibernate.tool.reveng.api.xml.XMLPrettyPrinterStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class AbstractXMLPrettyPrinterStrategy implements XMLPrettyPrinterStrategy {

	private static final Logger LOGGER = Logger.getLogger( AbstractXMLPrettyPrinterStrategy.class.getName() );
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = createDocumentBuilderFactory();

	public Document newDocument(String xml, String encoding) throws SAXException, IOException, ParserConfigurationException {
		final Document document = DOCUMENT_BUILDER_FACTORY
				.newDocumentBuilder()
				.parse(new InputSource(new ByteArrayInputStream(xml.getBytes(encoding))));
		document.normalize();
		return document;
	}

	protected void removeWhitespace(final Document document) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
				document,
				XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
		}
	}

	private static DocumentBuilderFactory createDocumentBuilderFactory() {
		DocumentBuilderFactory result = null;
		try {
			result = DocumentBuilderFactory.newInstance(
					"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
					null);
			result.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		}
		catch (ParserConfigurationException e) {
			LOGGER.severe(
					"A ParserConfigurationException happened while setting the " +
					"'http://apache.org/xml/features/nonvalidating/load-external-dtd' feature" +
					"to false." );
			throw new RuntimeException(e);
		}
		return result;
	}

}
