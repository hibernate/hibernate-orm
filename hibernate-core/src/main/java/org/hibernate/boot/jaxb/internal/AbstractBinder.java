package org.hibernate.boot.jaxb.internal;

import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.cfgxml.internal.XmlFactoryUtil;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.internal.stax.BufferedXMLEventReader;
import org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.util.StringHelper;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBinder<T> implements Binder<T> {

	private final LocalXmlResourceResolver xmlResourceResolver;

	protected AbstractBinder(ResourceStreamLocator resourceStreamLocator) {
		this.xmlResourceResolver = new LocalXmlResourceResolver( resourceStreamLocator );
	}

	public abstract boolean isValidationEnabled();

	public abstract boolean isDtdAndExternalEntitiesAllowed();

	@Override
	public <X extends T> Binding<X> bind(InputStream stream, Origin origin) {
		final XMLEventReader eventReader = createReader( stream, origin );
		try {
			return doBind( eventReader, origin );
		}
		finally {
			try {
				eventReader.close();
			}
			catch (XMLStreamException e) {
				JAXB_LOGGER.unableToCloseStaxReader( e );
			}
		}
	}

	protected XMLEventReader createReader(InputStream stream, Origin origin) {
		try {
			// create a standard StAX reader
			final XMLEventReader staxReader = staxFactory().createXMLEventReader( stream );
			// and wrap it in a buffered reader (keeping 100 element sized buffer)
			return new BufferedXMLEventReader( staxReader, 100 );
		}
		catch ( XMLStreamException e ) {
			throw new MappingException( "Unable to create StAX reader", e, origin );
		}
	}

	@Override
	public <X extends T> Binding<X> bind(Source source, Origin origin) {
		final XMLEventReader eventReader = createReader( source, origin );
		return doBind( eventReader, origin );
	}

	protected XMLEventReader createReader(Source source, Origin origin) {
		try {
			// create a standard StAX reader
			final XMLEventReader staxReader = staxFactory().createXMLEventReader( source );
			// and wrap it in a buffered reader (keeping 100 element sized buffer)
			return new BufferedXMLEventReader( staxReader, 100 );
		}
		catch ( XMLStreamException e ) {
			throw new MappingException( "Unable to create StAX reader", e, origin );
		}
	}

	private <X extends T> Binding<X> doBind(XMLEventReader eventReader, Origin origin) {
		try {
			final RootElement rootElement = seekRootElement( eventReader, origin );
			try {
				return doBind( eventReader, rootElement.startElement(), origin );
			}
			catch (MappingException e) {
				if ( rootElement.declaresEntities() && !isDtdAndExternalEntitiesAllowed() ) {
					throw new MappingException(
							"The mapping document declares a DTD and/or external entities, but these are disabled by default for security reasons;"
									+ " to re-enable them, set '" + MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES
									+ "' to 'true' at your own risk",
							e,
							origin
					);
				}
				throw e;
			}
		}
		finally {
			try {
				eventReader.close();
			}
			catch (Exception e) {
				JAXB_LOGGER.unableToCloseStaxReader( e );
			}
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
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		XmlFactoryUtil.configureXmlFactory( staxFactory, isDtdAndExternalEntitiesAllowed() );
		staxFactory.setXMLResolver( xmlResourceResolver );
		return staxFactory;
	}

	protected StartElement seekRootElementStartEvent(XMLEventReader staxEventReader, Origin origin) {
		return seekRootElement( staxEventReader, origin ).startElement();
	}

	private RootElement seekRootElement(XMLEventReader staxEventReader, Origin origin) {
		XMLEvent rootElementStartEvent;
		boolean declaresEntities = false;
		try {
			rootElementStartEvent = staxEventReader.peek();
			while ( rootElementStartEvent != null && !rootElementStartEvent.isStartElement() ) {
				if ( rootElementStartEvent instanceof DTD dtd ) {
					declaresEntities = dtd.getDocumentTypeDeclaration().contains( "<!ENTITY" );
				}
				staxEventReader.nextEvent();
				rootElementStartEvent = staxEventReader.peek();
			}
		}
		catch ( Exception e ) {
			throw new MappingException( "Error accessing StAX stream", e, origin );
		}

		if ( rootElementStartEvent == null ) {
			throw new MappingException( "Could not locate root element", origin );
		}

		return new RootElement( rootElementStartEvent.asStartElement(), declaresEntities );
	}

	private record RootElement(StartElement startElement, boolean declaresEntities) {
	}

	protected abstract <X extends T> Binding<X> doBind(XMLEventReader staxEventReader, StartElement rootElementStartEvent, Origin origin);

	@SuppressWarnings("unused")
	protected static boolean hasNamespace(StartElement startElement) {
		return StringHelper.isNotEmpty( startElement.getName().getNamespaceURI() );
	}

	protected <X extends T> X jaxb(XMLEventReader reader, Schema xsd, JAXBContext jaxbContext, Origin origin) {
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();

		try {
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			if ( isValidationEnabled() ) {
				unmarshaller.setSchema( xsd );
			}
			else {
				unmarshaller.setSchema( null );
			}
			unmarshaller.setEventHandler( handler );

			//noinspection unchecked
			return (X) unmarshaller.unmarshal( reader );
		}
		catch ( JAXBException e ) {
			throw new MappingException(
					"Unable to perform unmarshalling at line number " + handler.getLineNumber()
							+ " and column " + handler.getColumnNumber()
							+ ". Message: " + handler.getMessage(),
					e,
					origin
			);
		}
	}


}
