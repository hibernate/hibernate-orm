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

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.jboss.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.hibernate.InvalidMappingException;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Handles reading mapping documents, both {@code hbm} and {@code orm} varieties.
 *
 * @author Steve Ebersole
 */
public class MappingReader {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MappingReader.class.getName()
	);

	public static final MappingReader INSTANCE = new MappingReader();

	/**
	 * Disallow direct instantiation.
	 * <p/>
	 * Eventually we perhaps need to have this configurable by the "configuration" and simply reference it
	 * from there (registry).  This would allow, for example, injection of the entity resolver to use as
	 * instance state.
	 */
	private MappingReader() {
	}

	public XmlDocument readMappingDocument(EntityResolver entityResolver, InputSource source, Origin origin) {
		// IMPL NOTE : this is the legacy logic as pulled from the old AnnotationConfiguration code

		Exception failure;
		ErrorLogger errorHandler = new ErrorLogger();

		SAXReader saxReader = new SAXReader();
		saxReader.setEntityResolver( entityResolver );
		saxReader.setErrorHandler( errorHandler );
		saxReader.setMergeAdjacentText( true );
		saxReader.setValidation( true );

		Document document = null;
		try {
			// first try with orm 2.0 xsd validation
			setValidationFor( saxReader, "orm_2_0.xsd" );
			document = saxReader.read( source );
			if ( errorHandler.hasErrors() ) {
				throw errorHandler.getErrors().get( 0 );
			}
			return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
		}
		catch ( Exception orm2Problem ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Problem parsing XML using orm 2 xsd : %s", orm2Problem.getMessage() );
			}
			failure = orm2Problem;
			errorHandler.reset();

			if ( document != null ) {
				// next try with orm 1.0 xsd validation
				try {
					setValidationFor( saxReader, "orm_1_0.xsd" );
					document = saxReader.read( new StringReader( document.asXML() ) );
					if ( errorHandler.hasErrors() ) {
						errorHandler.logErrors();
						throw errorHandler.getErrors().get( 0 );
					}
					return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
				}
				catch ( Exception orm1Problem ) {
					if ( LOG.isDebugEnabled() ) {
						LOG.debugf( "Problem parsing XML using orm 1 xsd : %s", orm1Problem.getMessage() );
					}
				}
			}
		}
		throw new InvalidMappingException( "Unable to read XML", origin.getType(), origin.getName(), failure );
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

	// this is the version of the code I'd like to use, but it unfortunately works very differently between
	// JDK 1.5 ad JDK 1.6.  On 1.5 the vaildation "passes" even with invalid content.
	//
	// Options:
	// 		1) continue using the code above
	//		2) Document the issue on 1.5 and how to fix (specifying alternate SchemaFactory instance)
	//		3) Use a specific JAXP library (Xerces2, Saxon, Jing, MSV) and its SchemaFactory instance directly

//	public XmlDocument readMappingDocument(EntityResolver entityResolver, InputSource source, Origin origin) {
//		ErrorLogger errorHandler = new ErrorLogger();
//
//		SAXReader saxReader = new SAXReader( new DOMDocumentFactory() );
//		saxReader.setEntityResolver( entityResolver );
//		saxReader.setErrorHandler( errorHandler );
//		saxReader.setMergeAdjacentText( true );
//
//		Document documentTree = null;
//
//		// IMPL NOTE : here we enable DTD validation in case the mapping is a HBM file.  This will validate
//		// the document as it is parsed.  This is needed because the DTD defines default values that have to be
//		// applied as the document is parsed, so thats something we need to account for as we (if we) transition
//		// to XSD.
//		saxReader.setValidation( true );
//		try {
//			documentTree = saxReader.read( source );
//		}
//		catch ( DocumentException e ) {
//			// we had issues reading the input, most likely malformed document or validation error against DTD
//			throw new InvalidMappingException( "Unable to read XML", origin.getType(), origin.getName(), e );
//		}
//
//		Element rootElement = documentTree.getRootElement();
//		if ( rootElement ==  null ) {
//			throw new InvalidMappingException( "No root element", origin.getType(), origin.getName() );
//		}
//
//		if ( "entity-mappings".equals( rootElement.getName() ) ) {
//			final String explicitVersion = rootElement.attributeValue( "version" );
//			final String xsdVersionString = explicitVersion == null ? ASSUMED_ORM_XSD_VERSION : explicitVersion;
//			final SupportedOrmXsdVersion xsdVersion = SupportedOrmXsdVersion.parse( xsdVersionString );
//			final Schema schema = xsdVersion == SupportedOrmXsdVersion.ORM_1_0 ? orm1Schema() : orm2Schema();
//			try {
//				schema.newValidator().validate( new DOMSource( (org.w3c.dom.Document) documentTree ) );
//			}
//			catch ( SAXException e ) {
//				throw new InvalidMappingException( "Validation problem", origin.getType(), origin.getName(), e );
//			}
//			catch ( IOException e ) {
//				throw new InvalidMappingException( "Validation problem", origin.getType(), origin.getName(), e );
//			}
//		}
//		else {
//			if ( errorHandler.getError() != null ) {
//				throw new InvalidMappingException(
//						"Error validating hibernate-mapping against DTD",
//						origin.getType(),
//						origin.getName(),
//						errorHandler.getError()
//				);
//			}
//		}
//
//		return new XmlDocumentImpl( documentTree, origin );
//	}
//
//	public static enum SupportedOrmXsdVersion {
//		ORM_1_0,
//		ORM_2_0;
//
//		public static SupportedOrmXsdVersion parse(String name) {
//			if ( "1.0".equals( name ) ) {
//				return ORM_1_0;
//			}
//			else if ( "2.0".equals( name ) ) {
//				return ORM_2_0;
//			}
//			throw new IllegalArgumentException( "Unsupported orm.xml XSD version encountered [" + name + "]" );
//		}
//	}
//
//
//	public static final String ORM_1_SCHEMA_NAME = "org/hibernate/ejb/orm_1_0.xsd";
//	public static final String ORM_2_SCHEMA_NAME = "org/hibernate/ejb/orm_2_0.xsd";
//
//	private static Schema orm1Schema;
//
//	private static Schema orm1Schema() {
//		if ( orm1Schema == null ) {
//			orm1Schema = resolveLocalSchema( ORM_1_SCHEMA_NAME );
//		}
//		return orm1Schema;
//	}
//
//	private static Schema orm2Schema;
//
//	private static Schema orm2Schema() {
//		if ( orm2Schema == null ) {
//			orm2Schema = resolveLocalSchema( ORM_2_SCHEMA_NAME );
//		}
//		return orm2Schema;
//	}
//
//	private static Schema resolveLocalSchema(String schemaName) {
//		return resolveLocalSchema( schemaName, XMLConstants.W3C_XML_SCHEMA_NS_URI );
//	}
//
//	private static Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
//        URL url = ConfigHelper.findAsResource( schemaName );
//		if ( url == null ) {
//			throw new MappingException( "Unable to locate schema [" + schemaName + "] via classpath" );
//		}
//		try {
//			InputStream schemaStream = url.openStream();
//			try {
//				StreamSource source = new StreamSource(url.openStream());
//				SchemaFactory schemaFactory = SchemaFactory.newInstance( schemaLanguage );
//				return schemaFactory.newSchema(source);
//			}
//			catch ( SAXException e ) {
//				throw new MappingException( "Unable to load schema [" + schemaName + "]", e );
//			}
//			catch ( IOException e ) {
//				throw new MappingException( "Unable to load schema [" + schemaName + "]", e );
//			}
//			finally {
//				try {
//					schemaStream.close();
//				}
//				catch ( IOException e ) {
//					log.warn( "Problem closing schema stream [{}]", e.toString() );
//				}
//			}
//		}
//		catch ( IOException e ) {
//			throw new MappingException( "Stream error handling schema url [" + url.toExternalForm() + "]" );
//		}
//
//	}
}
