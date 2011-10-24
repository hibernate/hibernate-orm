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
package org.hibernate.service.internal;

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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.logging.Logger;
import org.xml.sax.SAXException;

import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.XsdException;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Steve Ebersole
 */
public class JaxbProcessor {
	private static final Logger log = Logger.getLogger( JaxbProcessor.class );

	private final ClassLoaderService classLoaderService;

	public JaxbProcessor(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	public JaxbHibernateConfiguration unmarshal(InputStream stream, Origin origin) {
		try {
			XMLStreamReader staxReader = staxFactory().createXMLStreamReader( stream );
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

	@SuppressWarnings( { "unchecked" })
	private JaxbHibernateConfiguration unmarshal(XMLStreamReader staxReader, final Origin origin) {
		final Object target;
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( JaxbHibernateConfiguration.class );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( schema() );
			unmarshaller.setEventHandler( handler );
			target = unmarshaller.unmarshal( staxReader );
			return (JaxbHibernateConfiguration) target;
		}
		catch ( JAXBException e ) {
			StringBuilder builder = new StringBuilder();
			builder.append( "Unable to perform unmarshalling at line number " )
					.append( handler.getLineNumber() )
					.append( " and column " )
					.append( handler.getColumnNumber() )
					.append( " in " ).append( origin.getType().name() ).append( " " ).append( origin.getName() )
					.append( ". Message: " )
					.append( handler.getMessage() );
			throw new ConfigurationException( builder.toString(), e );
		}
	}

	private Schema schema;

	private Schema schema() {
		if ( schema == null ) {
			schema = resolveLocalSchema( "org/hibernate/hibernate-configuration-4.0.xsd" );
		}
		return schema;
	}

	private Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( schemaName, XMLConstants.W3C_XML_SCHEMA_NS_URI );
	}

	private Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
		URL url = classLoaderService.locateResource( schemaName );
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
