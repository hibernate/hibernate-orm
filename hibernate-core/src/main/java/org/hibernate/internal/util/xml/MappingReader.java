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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.hibernate.InvalidMappingException;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.dom4j.io.STAXEventReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

	public XmlDocument readMappingDocument(InputSource source, Origin origin) {
		XMLEventReader staxReader = buildStaxEventReader( source, origin );
		try {
			return read( staxReader, origin );
		}
		finally {
			try {
				staxReader.close();
			}
			catch ( Exception ignore ) {
			}
		}
	}

	private XMLEventReader buildStaxEventReader(InputSource source, Origin origin) {
		XMLEventReader reader = null;

		if ( source.getByteStream() != null ) {
			try {
				reader = staxFactory().createXMLEventReader( source.getByteStream() );
			}
			catch (XMLStreamException e) {
				throw new XmlInfrastructureException(
						"Unable to create stax reader, origin = " + toLoggableString( origin ),
						e
				);
			}
		}
		else if ( source.getCharacterStream() != null ) {
			try {
				reader = staxFactory().createXMLEventReader( source.getCharacterStream() );
			}
			catch (XMLStreamException e) {
				throw new XmlInfrastructureException(
						"Unable to create stax reader, origin = " + toLoggableString( origin ),
						e
				);
			}
		}
		// todo : try to interpret the InputSource SystemId or Origin path?

		if ( reader == null ) {
			throw new XmlInfrastructureException( "Unable to convert SAX InputStream into StAX XMLEventReader" );
		}

		// For performance we wrap the reader in a buffered reader
		return new BufferedXMLEventReader( reader );
	}

	private XMLInputFactory staxFactory;

	private XMLInputFactory staxFactory() {
		if ( staxFactory == null ) {
			staxFactory = buildStaxFactory();
		}
		return staxFactory;
	}

	@SuppressWarnings( { "UnnecessaryLocalVariable" })
	private XMLInputFactory buildStaxFactory() {
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		staxFactory.setXMLResolver( LocalXmlResourceResolver.INSTANCE );
		return staxFactory;
	}

	private String toLoggableString(Origin origin) {
		return "[type=" + origin.getType() + ", name=" + origin.getName() + "]";
	}

	private static final QName ORM_VERSION_ATTRIBUTE_QNAME = new QName( "version" );

	private XmlDocument read(XMLEventReader staxEventReader, Origin origin) {
		XMLEvent event;
		try {
			event = staxEventReader.peek();
			while ( event != null && !event.isStartElement() ) {
				staxEventReader.nextEvent();
				event = staxEventReader.peek();
			}
		}
		catch ( Exception e ) {
			throw new InvalidMappingException( "Error accessing stax stream", origin, e );
		}

		if ( event == null ) {
			throw new InvalidMappingException( "Could not locate root element", origin );
		}

		final String rootElementName = event.asStartElement().getName().getLocalPart();

		if ( "entity-mappings".equals( rootElementName ) ) {
			final Attribute attribute = event.asStartElement().getAttributeByName( ORM_VERSION_ATTRIBUTE_QNAME );
			final String explicitVersion = attribute == null ? null : attribute.getValue();
			validateMapping(
					SupportedOrmXsdVersion.parse( explicitVersion, origin ),
					staxEventReader,
					origin
			);
		}

		return new XmlDocumentImpl( toDom4jDocument( staxEventReader, origin ), origin );
	}

	private Document toDom4jDocument(XMLEventReader staxEventReader, Origin origin) {
		STAXEventReader dom4jStaxEventReader = new STAXEventReader();
		try {
			// the dom4j converter class is touchy about comments (aka, comments make it implode)
			// so wrap the event stream in a filtering stream to filter out comment events
			staxEventReader = new FilteringXMLEventReader( staxEventReader ) {
				@Override
				protected XMLEvent filterEvent(XMLEvent event, boolean peek) {
					return event.getEventType() == XMLStreamConstants.COMMENT
							? null
							: event;
				}
			};

			return dom4jStaxEventReader.readDocument( staxEventReader );
		}
		catch (XMLStreamException e) {
			throw new InvalidMappingException( "Unable to read StAX source as dom4j Document for processing", origin, e );
		}
	}

	private void validateMapping(SupportedOrmXsdVersion xsdVersion, XMLEventReader staxEventReader, Origin origin) {
		final Validator validator = xsdVersion.getSchema().newValidator();
		final StAXSource staxSource;
		try {
			staxSource = new StAXSource( staxEventReader );
		}
		catch (XMLStreamException e) {
			throw new InvalidMappingException( "Unable to generate StAXSource from mapping", origin, e );
		}

		try {
			validator.validate( staxSource );
		}
		catch (SAXException e) {
			throw new InvalidMappingException( "SAXException performing validation", origin, e );
		}
		catch (IOException e) {
			throw new InvalidMappingException( "IOException performing validation", origin, e );
		}
	}

	public static enum SupportedOrmXsdVersion {
		ORM_1_0( "org/hibernate/jpa/orm_1_0.xsd" ),
		ORM_2_0( "org/hibernate/jpa/orm_2_0.xsd" ),
		ORM_2_1( "org/hibernate/jpa/orm_2_1.xsd" );

		private final String schemaResourceName;

		private SupportedOrmXsdVersion(String schemaResourceName) {
			this.schemaResourceName = schemaResourceName;
		}

		public static SupportedOrmXsdVersion parse(String name, Origin origin) {
			if ( "1.0".equals( name ) ) {
				return ORM_1_0;
			}
			else if ( "2.0".equals( name ) ) {
				return ORM_2_0;
			}
			else if ( "2.1".equals( name ) ) {
				return ORM_2_1;
			}
			throw new UnsupportedOrmXsdVersionException( name, origin );
		}

		private URL schemaUrl;

		public URL getSchemaUrl() {
			if ( schemaUrl == null ) {
				schemaUrl = resolveLocalSchemaUrl( schemaResourceName );
			}
			return schemaUrl;
		}

		private Schema schema;

		public Schema getSchema() {
			if ( schema == null ) {
				schema = resolveLocalSchema( getSchemaUrl() );
			}
			return schema;
		}
	}

	private static URL resolveLocalSchemaUrl(String schemaName) {
		URL url = MappingReader.class.getClassLoader().getResource( schemaName );
		if ( url == null ) {
			throw new XmlInfrastructureException( "Unable to locate schema [" + schemaName + "] via classpath" );
		}
		return url;
	}

	private static Schema resolveLocalSchema(URL schemaUrl) {

		try {
			InputStream schemaStream = schemaUrl.openStream();
			try {
				StreamSource source = new StreamSource(schemaUrl.openStream());
				SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				return schemaFactory.newSchema(source);
			}
			catch ( Exception e ) {
				throw new XmlInfrastructureException( "Unable to load schema [" + schemaUrl.toExternalForm() + "]", e );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					LOG.debugf( "Problem closing schema stream - %s", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XmlInfrastructureException( "Stream error handling schema url [" + schemaUrl.toExternalForm() + "]" );
		}

	}


	public XmlDocument readMappingDocument(EntityResolver entityResolver, InputSource source, Origin origin) {
		return legacyReadMappingDocument( entityResolver, source, origin );
//		return readMappingDocument( source, origin );
	}

	private XmlDocument legacyReadMappingDocument(EntityResolver entityResolver, InputSource source, Origin origin) {
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
			// first try with orm 2.1 xsd validation
			setValidationFor( saxReader, "orm_2_1.xsd" );
			document = saxReader.read( source );
			if ( errorHandler.hasErrors() ) {
				throw errorHandler.getErrors().get( 0 );
			}
			return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
		}
		catch ( Exception e ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Problem parsing XML using orm 2.1 xsd, trying 2.0 xsd : %s", e.getMessage() );
			}
			failure = e;
			errorHandler.reset();

			if ( document != null ) {
				// next try with orm 2.0 xsd validation
				try {
					setValidationFor( saxReader, "orm_2_0.xsd" );
					document = saxReader.read( new StringReader( document.asXML() ) );
					if ( errorHandler.hasErrors() ) {
						errorHandler.logErrors();
						throw errorHandler.getErrors().get( 0 );
					}
					return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
				}
				catch ( Exception e2 ) {
					if ( LOG.isDebugEnabled() ) {
						LOG.debugf( "Problem parsing XML using orm 2.0 xsd, trying 1.0 xsd : %s", e2.getMessage() );
					}
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
						catch ( Exception e3 ) {
							if ( LOG.isDebugEnabled() ) {
								LOG.debugf( "Problem parsing XML using orm 1.0 xsd : %s", e3.getMessage() );
							}
						}
					}
				}
			}
		}
		throw new InvalidMappingException( "Unable to read XML", origin.getType(), origin.getName(), failure );
	}

	private void setValidationFor(SAXReader saxReader, String xsd) {
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
			// saxReader.setFeature( "http://apache.org/xml/features/validation/dynamic", true );
			if ( "orm_2_1.xsd".equals( xsd ) ) {
				saxReader.setProperty(
						"http://apache.org/xml/properties/schema/external-schemaLocation",
						"http://xmlns.jcp.org/xml/ns/persistence/orm " + xsd
				);
			}
			else {
				saxReader.setProperty(
						"http://apache.org/xml/properties/schema/external-schemaLocation",
						"http://java.sun.com/xml/ns/persistence/orm " + xsd
				);
			}
		}
		catch ( SAXException e ) {
			saxReader.setValidation( false );
		}
	}

}
