/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2017-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.xml;

import org.hibernate.tool.api.xml.XMLPrettyPrinterStrategy;
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
