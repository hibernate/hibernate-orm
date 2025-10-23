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

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.logging.Logger;

public class TrAXPrettyPrinterStrategy extends AbstractXMLPrettyPrinterStrategy {

    private static final Logger LOGGER = Logger.getLogger( TrAXPrettyPrinterStrategy.class.getName() );
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance(
            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
            null);

    private int indent = 4;
    private boolean omitXmlDeclaration;

    @Override
    public String prettyPrint(String xml) throws Exception {
        final Document document = newDocument(xml, "UTF-8");
        removeWhitespace(document);

        final Transformer transformer = newTransformer(document);

        final StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    protected Transformer newTransformer(final Document document) throws TransformerConfigurationException {

        final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, isOmitXmlDeclaration() ? "yes" : "no");
        try {
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(getIndent()));
        }
        catch (IllegalArgumentException e) {
            LOGGER.severe( "An IllegalArgumentException happened while adding the 'indent' property." );
            throw new RuntimeException(e);
        }

        final DocumentType doctype = document.getDoctype();
        if (doctype != null) {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        }

        return transformer;
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }

    public boolean isOmitXmlDeclaration() {
        return omitXmlDeclaration;
    }

    public void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
        this.omitXmlDeclaration = omitXmlDeclaration;
    }

}
