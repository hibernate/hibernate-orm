/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal;

import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.internal.stax.BufferedXMLEventReader;
import org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBinder implements Binder {
	private static final Logger log = Logger.getLogger( AbstractBinder.class );

	private final LocalXmlResourceResolver xmlResourceResolver;
	private final boolean validateXml;

	protected AbstractBinder(ClassLoaderService classLoaderService) {
		this( classLoaderService, true );
	}

	protected AbstractBinder(ClassLoaderService classLoaderService, boolean validateXml) {
		this.xmlResourceResolver = new LocalXmlResourceResolver( classLoaderService );
		this.validateXml = validateXml;
	}

	public boolean isValidationEnabled() {
		return validateXml;
	}

	@Override
	public Binding bind(InputStream stream, Origin origin) {
		final XMLEventReader eventReader = createReader( stream, origin );
		try {
			return doBind( eventReader, origin );
		}
		finally {
			try {
				eventReader.close();
			}
			catch (XMLStreamException e) {
				log.debug( "Unable to close StAX reader", e );
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
			throw new MappingException( "Unable to create stax reader", e, origin );
		}
	}

	@Override
	public Binding bind(Source source, Origin origin) {
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
			throw new MappingException( "Unable to create stax reader", e, origin );
		}
	}

	private Binding doBind(XMLEventReader eventReader, Origin origin) {
		try {
			final StartElement rootElementStartEvent = seekRootElementStartEvent( eventReader, origin );
			return doBind( eventReader, rootElementStartEvent, origin );
		}
		finally {
			try {
				eventReader.close();
			}
			catch ( Exception e ) {
				log.debug( "Unable to close StAX reader", e );

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

	@SuppressWarnings( { "UnnecessaryLocalVariable" })
	private XMLInputFactory buildStaxFactory() {
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		staxFactory.setXMLResolver( xmlResourceResolver );
		return staxFactory;
	}

	protected StartElement seekRootElementStartEvent(XMLEventReader staxEventReader, Origin origin) {
		XMLEvent rootElementStartEvent;
		try {
			rootElementStartEvent = staxEventReader.peek();
			while ( rootElementStartEvent != null && !rootElementStartEvent.isStartElement() ) {
				staxEventReader.nextEvent();
				rootElementStartEvent = staxEventReader.peek();
			}
		}
		catch ( Exception e ) {
			throw new MappingException( "Error accessing stax stream", e, origin );
		}

		if ( rootElementStartEvent == null ) {
			throw new MappingException( "Could not locate root element", origin );
		}

		return rootElementStartEvent.asStartElement();
	}

	protected abstract Binding doBind(XMLEventReader staxEventReader, StartElement rootElementStartEvent, Origin origin);

	protected static boolean hasNamespace(StartElement startElement) {
		return ! "".equals( startElement.getName().getNamespaceURI() );
	}

	@SuppressWarnings("unchecked")
	protected <T> T jaxb(XMLEventReader reader, Schema xsd, JAXBContext jaxbContext, Origin origin) {
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

			return (T) unmarshaller.unmarshal( reader );
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
