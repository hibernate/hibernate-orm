package org.hibernate.boot.cfgxml.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.xml.XsdException;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

import org.xml.sax.SAXException;

import jakarta.annotation.Nullable;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class JaxbCfgProcessor {

	public static final String HIBERNATE_CONFIGURATION_URI = "http://www.hibernate.org/xsd/orm/cfg";

	private final ClassLoaderService classLoaderService;

	/**
	 * Settings gathered (from system properties and {@code /hibernate.properties}) before
	 * {@code hibernate.cfg.xml} is parsed. Null when no such settings source is available, in
	 * which case the XML-security-related settings fall back to their secure defaults.
	 */
	private final @Nullable Map<String, Object> settings;

	public JaxbCfgProcessor(ClassLoaderService classLoaderService) {
		this( classLoaderService, null );
	}

	public JaxbCfgProcessor(ClassLoaderService classLoaderService, @Nullable Map<String, Object> settings) {
		this.classLoaderService = classLoaderService;
		this.settings = settings;
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
			staxFactory = buildStaxFactory( isDtdAndExternalEntitiesAllowed() );
		}
		return staxFactory;
	}

	private static XMLInputFactory buildStaxFactory(boolean allowDtdAndExternalEntities) {
		final var staxFactory = XMLInputFactory.newInstance();
		XmlFactoryUtil.configureXmlFactory( staxFactory, allowDtdAndExternalEntities );

		return staxFactory;
	}

	private boolean isDtdAndExternalEntitiesAllowed() {
		return readBooleanSetting( MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES );
	}

	private boolean readBooleanSetting(String settingName) {
		if ( settings == null ) {
			return false;
		}
		final Object setting = settings.get( settingName );
		return setting != null && BOOLEAN.convert( setting );
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
		final URL url = classLoaderService.locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			final var schemaStream = url.openStream();
			try {
				return XmlFactoryUtil.newSchemaFactory()
						.newSchema( new StreamSource( schemaStream ) );
			}
			catch ( SAXException e ) {
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
