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
package org.hibernate.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.hibernate.HibernateException;
import org.hibernate.InvalidMappingException;

/**
 * Small helper class that lazy loads DOM and SAX reader and keep them for fast use afterwards.
 */
public final class XMLHelper {
	public static final String ORM_1_SCHEMA_NAME = "org/hibernate/ejb/orm_1_0.xsd";
	public static final String ORM_2_SCHEMA_NAME = "org/hibernate/ejb/orm_2_0.xsd";

	private static final Logger log = LoggerFactory.getLogger(XMLHelper.class);
	public static final EntityResolver DEFAULT_DTD_RESOLVER = new DTDEntityResolver();

	private DOMReader domReader;
	private SAXReader saxReader;

	/**
	 * Create a dom4j SAXReader which will append all validation errors
	 * to errorList
	 */
	public SAXReader createSAXReader(String file, List errorsList, EntityResolver entityResolver) {
		SAXReader saxReader = resolveSAXReader();
		saxReader.setEntityResolver(entityResolver);
		saxReader.setErrorHandler( new ErrorLogger(file, errorsList) );
		return saxReader;
	}

	private SAXReader resolveSAXReader() {
		if ( saxReader == null ) {
			saxReader = new SAXReader();
			saxReader.setMergeAdjacentText(true);
			saxReader.setValidation(true);
		}
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
		private List<SAXParseException> errors;

		private ErrorLogger(String file, List errors) {
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

	public static interface MetadataXmlSource {
		public static interface Origin {
			public String getType();
			public String getName();
		}
		public Origin getOrigin();
		public InputSource getInputSource();
	}

	public static interface MetadataXml extends Serializable {
		public boolean isOrmXml();
		public Document getXmlDocument();
		public String getOriginType();
		public String getOriginName();
	}

	private static class MetadataXmlImpl implements MetadataXml, Serializable {
		private final Document xmlDocument;
		private final boolean isOrmXml;
		private final String originType;
		private final String originName;

		private MetadataXmlImpl(Document xmlDocument, String originType, String originName) {
			this.xmlDocument = xmlDocument;
			this.originType = originType;
			this.originName = originName;
			this.isOrmXml = "entity-mappings".equals( xmlDocument.getRootElement().getName() );
		}

		public Document getXmlDocument() {
			return xmlDocument;
		}

		public boolean isOrmXml() {
			return isOrmXml;
		}

		public String getOriginType() {
			return originType;
		}

		public String getOriginName() {
			return originName;
		}
	}

	public MetadataXml buildMetadataXml(Document xmlDocument, String originType, String originName) {
		return new MetadataXmlImpl( xmlDocument, originType, originName );
	}

	public MetadataXml readMappingDocument(EntityResolver entityResolver, MetadataXmlSource source) {
		// IMPL NOTE : this is the legacy logic as pulled from the old AnnotationConfiguration code

		Exception failure;
		ErrorLogger2 errorHandler = new ErrorLogger2();

		SAXReader saxReader = new SAXReader();
		saxReader.setEntityResolver( entityResolver );
		saxReader.setErrorHandler( errorHandler );
		saxReader.setMergeAdjacentText( true );
		saxReader.setValidation( true );

		Document document = null;
		try {
			// first try with orm 2.0 xsd validation
			setValidationFor( saxReader, "orm_2_0.xsd" );
			document = saxReader.read( source.getInputSource() );
			if ( errorHandler.error != null ) {
				throw errorHandler.error;
			}
			return buildMetadataXml( document, source.getOrigin().getType(), source.getOrigin().getName() );
		}
		catch ( Exception orm2Problem ) {
			log.debug( "Problem parsing XML using orm 2 xsd : {}", orm2Problem.getMessage() );
			failure = orm2Problem;
			errorHandler.error = null;

			if ( document != null ) {
				// next try with orm 1.0 xsd validation
				try {
					setValidationFor( saxReader, "orm_1_0.xsd" );
					document = saxReader.read(  new StringReader( document.asXML() ) );
					if ( errorHandler.error != null ) {
						throw errorHandler.error;
					}
					return buildMetadataXml( document, source.getOrigin().getType(), source.getOrigin().getName() );
				}
				catch ( Exception orm1Problem ) {
					log.debug( "Problem parsing XML using orm 1 xsd : {}", orm1Problem.getMessage() );
					errorHandler.error = null;
				}
			}
		}
		throw new InvalidMappingException( "Unable to read XML", source.getOrigin().getType(), source.getOrigin().getName(), failure );

	}

	private void setValidationFor(SAXReader saxReader, String xsd) {
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
			//saxReader.setFeature( "http://apache.org/xml/features/validation/dynamic", true );
			//set the default schema locators
			saxReader.setProperty(
					"http://apache.org/xml/properties/schema/external-schemaLocation",
					"http://java.sun.com/xml/ns/persistence/orm " + xsd
			);
		}
		catch ( SAXException e ) {
			saxReader.setValidation( false );
		}
	}

//	public MetadataXml readMappingDocument(EntityResolver entityResolver, MetadataXmlSource source) {
//		Exception failure;
//		ErrorLogger2 errorHandler = new ErrorLogger2();
//
//		SAXReader saxReader = resolveSAXReader();
//		saxReader.setEntityResolver( entityResolver );
//		saxReader.setErrorHandler( errorHandler );
//
//		try {
//			Document document = saxReader.read( source.getInputSource() );
//			if ( errorHandler.error != null ) {
//				Exception problem = errorHandler.error;
//				errorHandler.error = null;
//				throw problem;
//			}
//		}
//		catch ( Exception parseError ) {
//			log.debug( "Problem parsing XML document : {}", parseError.getMessage() );
//		}
//		// first try with orm 2.0 xsd validation
//		try {
//			SAXReader saxReader = orm2SaxReader();
//			if ( errorHandler.error != null ) {
//				Exception problem = errorHandler.error;
//				errorHandler.error = null;
//				throw problem;
//			}
//			return buildMetadataXml( document, source.getOrigin().getType(), source.getOrigin().getName() );
//		}
//		catch ( Exception orm2Problem ) {
//			log.debug( "Problem parsing XML using orm 2 xsd : {}", orm2Problem.getMessage() );
//			failure = orm2Problem;
//
//			// next try with orm 1.0 xsd validation
//			try {
//				SAXReader saxReader = orm1SaxReader();
//				saxReader.setEntityResolver( entityResolver );
//				saxReader.setErrorHandler( errorHandler );
//				Document document = saxReader.read( source.getInputSource() );
//				if ( errorHandler.error != null ) {
//					Exception problem = errorHandler.error;
//					errorHandler.error = null;
//					throw problem;
//				}
//				return buildMetadataXml( document, source.getOrigin().getType(), source.getOrigin().getName() );
//			}
//			catch ( Exception orm1Problem ) {
//				log.debug( "Problem parsing XML using orm 1 xsd : {}", orm1Problem.getMessage() );
//			}
//		}
//		throw new InvalidMappingException( "Unable to read XML", source.getOrigin().getType(), source.getOrigin().getName(), failure );
//	}

	private static class ErrorLogger2 implements ErrorHandler {
		private SAXParseException error; // capture the initial error

		public void error(SAXParseException error) {
			log.error( "Error parsing XML (" + error.getLineNumber() + ") : " + error.getMessage() );
			if ( this.error == null ) {
				this.error = error;
			}
		}
		public void fatalError(SAXParseException error) {
			error( error );
		}
		public void warning(SAXParseException warn) {
			log.error( "Warning parsing XML (" + error.getLineNumber() + ") : " + error.getMessage() );
		}
	}

	private static SAXReader orm2SaxReader;

	private static SAXReader orm2SaxReader() throws IOException, SAXException {
		if ( orm2SaxReader == null ) {
			orm2SaxReader = buildReaderWithSchema( orm2Schema() );
		}
		return orm2SaxReader;
	}

	private static Schema orm2Schema;

	private static Schema orm2Schema() throws IOException, SAXException {
		if ( orm2Schema == null ) {
			orm2Schema = resolveLocalSchema( ORM_2_SCHEMA_NAME );
		}
		return orm2Schema;
	}

	private static Schema resolveLocalSchema(String schemaName) throws IOException, SAXException {
        URL url = ConfigHelper.findAsResource( schemaName );
        InputStream schemaStream = url.openStream();
        try {
            StreamSource source = new StreamSource(url.openStream());
            SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            return schemaFactory.newSchema(source);
        }
		finally {
            schemaStream.close();
        }
	}

	private static SAXReader buildReaderWithSchema(Schema schema) throws SAXException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setSchema( schema );
		try {
			SAXReader saxReader = new SAXReader( factory.newSAXParser().getXMLReader() );
			saxReader.setMergeAdjacentText( true );
			saxReader.setValidation( true );
			return saxReader;
		}
		catch ( ParserConfigurationException e ) {
			throw new HibernateException( "Unable to build SAXReader with XSD support", e );
		}
	}

	private static SAXReader orm1SaxReader;

	private static SAXReader orm1SaxReader() throws IOException, SAXException {
		if ( orm1SaxReader == null ) {
			orm1SaxReader = buildReaderWithSchema( orm1Schema() );
		}
		return orm1SaxReader;
	}

	private static Schema orm1Schema;

	private static Schema orm1Schema() throws IOException, SAXException {
		if ( orm1Schema == null ) {
			orm1Schema = resolveLocalSchema( ORM_1_SCHEMA_NAME );
		}
		return orm1Schema;
	}
}
