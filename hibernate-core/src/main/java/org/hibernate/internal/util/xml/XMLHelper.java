/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import org.hibernate.internal.util.ClassLoaderHelper;

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

		ClassLoader cl = ClassLoaderHelper.getContextClassLoader();
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
