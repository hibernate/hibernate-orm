/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.metamodel.source.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.hibernate.internal.jaxb.JaxbRoot;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityMappings;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.XsdException;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Helper class for unmarshalling xml configuration using StAX and JAXB.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class JaxbHelper {
	private static final Logger log = Logger.getLogger( JaxbHelper.class );

	public static final String ASSUMED_ORM_XSD_VERSION = "2.0";

	private final MetadataSources metadataSources;

	public JaxbHelper(MetadataSources metadataSources) {
		this.metadataSources = metadataSources;
	}

	public JaxbRoot unmarshal(InputStream stream, Origin origin) {
		try {
			XMLEventReader staxReader = staxFactory().createXMLEventReader( stream );
			try {
				return unmarshal( staxReader, origin );
			}
			finally {
				try {
					staxReader.close();
				}
				catch ( Exception ignore ) {
				}
			}
		}
		catch ( XMLStreamException e ) {
			throw new MappingException( "Unable to create stax reader", e, origin );
		}
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
		return staxFactory;
	}

	private static final QName ORM_VERSION_ATTRIBUTE_QNAME = new QName( "version" );

	@SuppressWarnings( { "unchecked" })
	private JaxbRoot unmarshal(XMLEventReader staxEventReader, final Origin origin) {
		XMLEvent event;
		try {
			event = staxEventReader.peek();
			while ( event != null && !event.isStartElement() ) {
				staxEventReader.nextEvent();
				event = staxEventReader.peek();
			}
		}
		catch ( Exception e ) {
			throw new MappingException( "Error accessing stax stream", e, origin );
		}

		if ( event == null ) {
			throw new MappingException( "Could not locate root element", origin );
		}

		final Schema validationSchema;
		final Class jaxbTarget;

		final String elementName = event.asStartElement().getName().getLocalPart();

		if ( "entity-mappings".equals( elementName ) ) {
			final Attribute attribute = event.asStartElement().getAttributeByName( ORM_VERSION_ATTRIBUTE_QNAME );
			final String explicitVersion = attribute == null ? null : attribute.getValue();
			validationSchema = resolveSupportedOrmXsd( explicitVersion );
			jaxbTarget = JaxbEntityMappings.class;
		}
		else {
			validationSchema = hbmSchema();
			jaxbTarget = JaxbHibernateMapping.class;
		}

		final Object target;
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( jaxbTarget );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( validationSchema );
			unmarshaller.setEventHandler( handler );
			target = unmarshaller.unmarshal( staxEventReader );
		}

		catch ( JAXBException e ) {
			StringBuilder builder = new StringBuilder();
			builder.append( "Unable to perform unmarshalling at line number " );
			builder.append( handler.getLineNumber() );
			builder.append( " and column " );
			builder.append( handler.getColumnNumber() );
			builder.append( ". Message: " );
			builder.append( handler.getMessage() );
			throw new MappingException( builder.toString(), e, origin );
		}

		return new JaxbRoot( target, origin );
	}

	@SuppressWarnings( { "unchecked" })
	public JaxbRoot unmarshal(Document document, Origin origin) {
		Element rootElement = document.getDocumentElement();
		if ( rootElement == null ) {
			throw new MappingException( "No root element found", origin );
		}

		final Schema validationSchema;
		final Class jaxbTarget;

		if ( "entity-mappings".equals( rootElement.getNodeName() ) ) {
			final String explicitVersion = rootElement.getAttribute( "version" );
			validationSchema = resolveSupportedOrmXsd( explicitVersion );
			jaxbTarget = JaxbEntityMappings.class;
		}
		else {
			validationSchema = hbmSchema();
			jaxbTarget = JaxbHibernateMapping.class;
		}

		final Object target;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( jaxbTarget );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( validationSchema );
			target = unmarshaller.unmarshal( new DOMSource( document ) );
		}
		catch ( JAXBException e ) {
			throw new MappingException( "Unable to perform unmarshalling", e, origin );
		}

		return new JaxbRoot( target, origin );
	}

	private Schema resolveSupportedOrmXsd(String explicitVersion) {
		final String xsdVersionString = explicitVersion == null ? ASSUMED_ORM_XSD_VERSION : explicitVersion;
		if ( "1.0".equals( xsdVersionString ) ) {
			return orm1Schema();
		}
		else if ( "2.0".equals( xsdVersionString ) ) {
			return orm2Schema();
		}
		throw new IllegalArgumentException( "Unsupported orm.xml XSD version encountered [" + xsdVersionString + "]" );
	}

	public static final String HBM_SCHEMA_NAME = "org/hibernate/hibernate-mapping-4.0.xsd";
	public static final String ORM_1_SCHEMA_NAME = "org/hibernate/ejb/orm_1_0.xsd";
	public static final String ORM_2_SCHEMA_NAME = "org/hibernate/ejb/orm_2_0.xsd";

	private Schema hbmSchema;

	private Schema hbmSchema() {
		if ( hbmSchema == null ) {
			hbmSchema = resolveLocalSchema( HBM_SCHEMA_NAME );
		}
		return hbmSchema;
	}

	private Schema orm1Schema;

	private Schema orm1Schema() {
		if ( orm1Schema == null ) {
			orm1Schema = resolveLocalSchema( ORM_1_SCHEMA_NAME );
		}
		return orm1Schema;
	}

	private Schema orm2Schema;

	private Schema orm2Schema() {
		if ( orm2Schema == null ) {
			orm2Schema = resolveLocalSchema( ORM_2_SCHEMA_NAME );
		}
		return orm2Schema;
	}

	private Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( schemaName, XMLConstants.W3C_XML_SCHEMA_NS_URI );
	}

	private Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
		URL url = metadataSources.getServiceRegistry()
				.getService( ClassLoaderService.class )
				.locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			InputStream schemaStream = url.openStream();
			try {
				StreamSource source = new StreamSource( url.openStream() );
				SchemaFactory schemaFactory = SchemaFactory.newInstance( schemaLanguage );
				return schemaFactory.newSchema( source );
			}
			catch ( SAXException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			catch ( IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					log.debugf( "Problem closing schema stream [%s]", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XsdException( "Stream error handling schema url [" + url.toExternalForm() + "]", schemaName );
		}
	}

	static class ContextProvidingValidationEventHandler implements ValidationEventHandler {
		private int lineNumber;
		private int columnNumber;
		private String message;

		@Override
		public boolean handleEvent(ValidationEvent validationEvent) {
			ValidationEventLocator locator = validationEvent.getLocator();
			lineNumber = locator.getLineNumber();
			columnNumber = locator.getColumnNumber();
			message = validationEvent.getMessage();
			return false;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public int getColumnNumber() {
			return columnNumber;
		}

		public String getMessage() {
			return message;
		}
	}
}
