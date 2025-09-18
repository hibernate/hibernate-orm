/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.cfgxml.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.xml.XsdException;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class JaxbCfgProcessor {

	public static final String HIBERNATE_CONFIGURATION_URI = "http://www.hibernate.org/xsd/orm/cfg";

	private final ClassLoaderService classLoaderService;
	private final LocalXmlResourceResolver xmlResourceResolver;

	public JaxbCfgProcessor(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
		this.xmlResourceResolver = new LocalXmlResourceResolver( classLoaderService );
	}

	public JaxbCfgHibernateConfiguration unmarshal(InputStream stream, Origin origin) {
		try {
			final var staxReader = staxFactory().createXMLEventReader( stream );
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
			throw new HibernateException( "Unable to create StAX reader", e );
		}
	}

	private XMLInputFactory staxFactory;

	private XMLInputFactory staxFactory() {
		if ( staxFactory == null ) {
			staxFactory = buildStaxFactory();
		}
		return staxFactory;
	}

	private XMLInputFactory buildStaxFactory() {
		final var staxFactory = XMLInputFactory.newInstance();
		staxFactory.setXMLResolver( xmlResourceResolver );
		return staxFactory;
	}

	private JaxbCfgHibernateConfiguration unmarshal(XMLEventReader staxEventReader, final Origin origin) {
		final XMLEvent event = xmlEvent( staxEventReader );
		if ( !isNamespaced( event.asStartElement() ) ) {
			// if the elements are not namespaced, wrap the reader in a reader which will namespace them as pulled.
			BOOT_LOGGER.cfgXmlDocumentDidNotDefineNamespaces();
			staxEventReader = new NamespaceAddingEventReader( staxEventReader, HIBERNATE_CONFIGURATION_URI );
		}

		final var handler = new ContextProvidingValidationEventHandler();
		try {
			final var unmarshaller =
					JAXBContext.newInstance( JaxbCfgHibernateConfiguration.class )
							.createUnmarshaller();
			unmarshaller.setSchema( schema() );
			unmarshaller.setEventHandler( handler );
			return (JaxbCfgHibernateConfiguration) unmarshaller.unmarshal( staxEventReader );
		}
		catch ( JAXBException e ) {
			throw new ConfigurationException(
					"Unable to perform unmarshalling at line number " + handler.getLineNumber()
							+ " and column " + handler.getColumnNumber()
							+ " in " + origin.getType().name() + " " + origin.getName()
							+ ". Message: " + handler.getMessage(), e
			);
		}
	}

	private static XMLEvent xmlEvent(XMLEventReader staxEventReader) {
		try {
			XMLEvent event = staxEventReader.peek();
			while ( event != null && !event.isStartElement() ) {
				staxEventReader.nextEvent();
				event = staxEventReader.peek();
			}
			if ( event == null ) {
				throw new HibernateException( "Could not locate root element" );
			}
			return event;
		}
		catch ( Exception e ) {
			throw new HibernateException( "Error accessing StAX stream", e );
		}
	}

	private boolean isNamespaced(StartElement startElement) {
		return isNotEmpty( startElement.getName().getNamespaceURI() );
	}

	private Schema schema;

	private Schema schema() {
		if ( schema == null ) {
			schema = resolveLocalSchema( "org/hibernate/hibernate-configuration-4.0.xsd" );
		}
		return schema;
	}

	private Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( schemaName, W3C_XML_SCHEMA_NS_URI );
	}

	private Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
		final URL url = classLoaderService.locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			final var schemaStream = url.openStream();
			try {
				return SchemaFactory.newInstance( schemaLanguage )
						.newSchema( new StreamSource( url.openStream() ) );
			}
			catch ( SAXException | IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					JAXB_LOGGER.problemClosingSchemaStream( e.toString() );
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
			final var locator = validationEvent.getLocator();
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

	public static class NamespaceAddingEventReader extends EventReaderDelegate {
		private final XMLEventFactory xmlEventFactory;
		private final String namespaceUri;

		public NamespaceAddingEventReader(XMLEventReader reader, String namespaceUri) {
			this( reader, XMLEventFactory.newInstance(), namespaceUri );
		}

		public NamespaceAddingEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory, String namespaceUri) {
			super( reader );
			this.xmlEventFactory = xmlEventFactory;
			this.namespaceUri = namespaceUri;
		}

		private StartElement withNamespace(StartElement startElement) {
			// otherwise, wrap the start element event to provide a default namespace mapping
			final List<Namespace> namespaces = new ArrayList<>();
			namespaces.add( xmlEventFactory.createNamespace( "", namespaceUri ) );
			final var originalNamespaces = startElement.getNamespaces();
			while ( originalNamespaces.hasNext() ) {
				namespaces.add( originalNamespaces.next() );
			}
			return xmlEventFactory.createStartElement(
					new QName( namespaceUri, startElement.getName().getLocalPart() ),
					startElement.getAttributes(),
					namespaces.iterator()
			);
		}

		@Override
		public XMLEvent nextEvent() throws XMLStreamException {
			final var event = super.nextEvent();
			return event.isStartElement() ? withNamespace( event.asStartElement() ) : event;
		}

		@Override
		public XMLEvent peek() throws XMLStreamException {
			final var event = super.peek();
			return event.isStartElement() ? withNamespace( event.asStartElement() ) : event;
		}
	}
}
