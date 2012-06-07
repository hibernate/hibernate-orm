/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal.util.xml;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

/**
 * Small helper class that lazy loads DOM and SAX reader and keep them for fast use afterwards.
 */
public final class XMLHelper {

	public static final EntityResolver DEFAULT_DTD_RESOLVER = new DTDEntityResolver();

	private DOMReader domReader;
	private SAXReader saxReader;

	/**
	 * @param errorHandler the sax error handler
	 * @param entityResolver an xml entity resolver
	 *
	 * @return Create and return a dom4j {@code SAXReader} which will append all validation errors
	 *         to the passed error list
	 */
	public SAXReader createSAXReader(ErrorHandler errorHandler, EntityResolver entityResolver) {
		SAXReader saxReader = resolveSAXReader();
		saxReader.setEntityResolver( entityResolver );
		saxReader.setErrorHandler( errorHandler );
		return saxReader;
	}

	private SAXReader resolveSAXReader() {
		if ( saxReader == null ) {
			saxReader = new SAXReader();
			saxReader.setMergeAdjacentText( true );
			saxReader.setValidation( true );
		}
		return saxReader;
	}

	/**
	 * @return create and return a dom4j DOMReader
	 */
	public DOMReader createDOMReader() {
		if ( domReader == null ) {
			domReader = new DOMReader();
		}
		return domReader;
	}

	public static Element generateDom4jElement(String elementName) {
		return getDocumentFactory().createElement( elementName );
	}

	public static DocumentFactory getDocumentFactory() {

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		DocumentFactory factory;
		try {
			Thread.currentThread().setContextClassLoader( XMLHelper.class.getClassLoader() );
			factory = DocumentFactory.getInstance();
		}
		finally {
			Thread.currentThread().setContextClassLoader( cl );
		}
		return factory;
	}

	public static void dump(Element element) {
		try {
			// try to "pretty print" it
			OutputFormat outFormat = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter( System.out, outFormat );
			writer.write( element );
			writer.flush();
			System.out.println( "" );
		}
		catch ( Throwable t ) {
			// otherwise, just dump it
			System.out.println( element.asXML() );
		}

	}
}
