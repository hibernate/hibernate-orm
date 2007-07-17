//$Id: XMLHelper.java 8563 2005-11-10 15:58:59Z steveebersole $
package org.hibernate.util;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.Element;
import org.dom4j.DocumentFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Small helper class that lazy loads DOM and SAX reader and keep them for fast use afterwards.
 *
 *
 *
 */
public final class XMLHelper {

	private static final Log log = LogFactory.getLog(XMLHelper.class);
	public static final EntityResolver DEFAULT_DTD_RESOLVER = new DTDEntityResolver();

	private DOMReader domReader;
	private SAXReader saxReader;

	/**
	 * Create a dom4j SAXReader which will append all validation errors
	 * to errorList
	 */
	public SAXReader createSAXReader(String file, List errorsList, EntityResolver entityResolver) {
		if (saxReader==null) saxReader = new SAXReader();
		saxReader.setEntityResolver(entityResolver);
		saxReader.setErrorHandler( new ErrorLogger(file, errorsList) );
		saxReader.setMergeAdjacentText(true);
		saxReader.setValidation(true);
		return saxReader;
	}

	/**
	 * Create a dom4j DOMReader
	 */
	public DOMReader createDOMReader() {
		if (domReader==null) domReader = new DOMReader();
		return domReader;
	}

	public static class ErrorLogger implements ErrorHandler {
		private String file;
		private List errors;
		ErrorLogger(String file, List errors) {
			this.file=file;
			this.errors = errors;
		}
		public void error(SAXParseException error) {
			log.error( "Error parsing XML: " + file + '(' + error.getLineNumber() + ") " + error.getMessage() );
			errors.add(error);
		}
		public void fatalError(SAXParseException error) {
			error(error);
		}
		public void warning(SAXParseException warn) {
			log.warn( "Warning parsing XML: " + file + '(' + warn.getLineNumber() + ") " + warn.getMessage() );
		}
	}

	public static Element generateDom4jElement(String elementName) {
		return DocumentFactory.getInstance().createElement( elementName );
	}

	public static void dump(Element element) {
		try {
			// try to "pretty print" it
			OutputFormat outformat = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter( System.out, outformat );
			writer.write( element );
			writer.flush();
			System.out.println( "" );
		}
		catch( Throwable t ) {
			// otherwise, just dump it
			System.out.println( element.asXML() );
		}

	}
}
