/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.hibernate.HibernateException;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.archive.internal.RepeatableInputStreamAccess;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.internal.stax.BufferedXMLEventReader;
import org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBinder<T> implements Binder<T> {
	private static final Logger log = Logger.getLogger( AbstractBinder.class );

	private final LocalXmlResourceResolver xmlResourceResolver;

	protected AbstractBinder(ResourceStreamLocator resourceStreamLocator) {
		this.xmlResourceResolver = new LocalXmlResourceResolver( resourceStreamLocator );
	}

	@Override
	public <X extends T> Binding<X> bind(InputStream stream, Origin origin) {
		return bind( new RepeatableInputStreamAccess(origin.getName(), stream), origin );
	}

	@Override
	public <X extends T> Binding<X> bind(InputStreamAccess streamAccess, Origin origin) {
		final JaxbBindingSource jaxbBindingSource = createReader( streamAccess, origin );
		try {
			return doBind( jaxbBindingSource );
		}
		finally {
			try {
				jaxbBindingSource.getEventReader().close();
			}
			catch (XMLStreamException e) {
				log.debug( "Unable to close StAX reader", e );
			}
		}
	}

	protected JaxbBindingSource createReader(InputStreamAccess streamAccess, Origin origin) {
			return new JaxbBindingSource() {
				@Override
				public Origin getOrigin() {
					return origin;
				}

				@Override
				public InputStreamAccess getInputStreamAccess() {
					return streamAccess;
				}

				@Override
				public XMLEventReader getEventReader() {
					try {
					// create a standard StAX reader
					final XMLEventReader staxReader = staxFactory().createXMLEventReader( streamAccess.accessInputStream() );
					// and wrap it in a buffered reader (keeping 100 element sized buffer)
					return new BufferedXMLEventReader( staxReader, 100 );
					}
					catch ( XMLStreamException e ) {
						throw new MappingException( "Unable to create StAX reader", e, origin );
					}
				}
			};
	}

	@Override
	public <X extends T> Binding<X> bind(Source source, Origin origin) {
		throw new HibernateException( "The Binder.bind(Source, Origin) method is no longer supported" );
	}

	private <X extends T> Binding<X> doBind(JaxbBindingSource jaxbBindingSource) {
		try {
			final StartElement rootElementStartEvent = seekRootElementStartEvent( jaxbBindingSource.getEventReader(), jaxbBindingSource.getOrigin() );
			return doBind( jaxbBindingSource, rootElementStartEvent );
		}
		finally {
			try {
				jaxbBindingSource.getEventReader().close();
			}
			catch (Exception e) {
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
			throw new MappingException( "Error accessing StAX stream", e, origin );
		}

		if ( rootElementStartEvent == null ) {
			throw new MappingException( "Could not locate root element", origin );
		}

		return rootElementStartEvent.asStartElement();
	}

	protected abstract <X extends T> Binding<X> doBind(JaxbBindingSource jaxbBindingSource, StartElement rootElementStartEvent);

	@SuppressWarnings("unused")
	protected static boolean hasNamespace(StartElement startElement) {
		return StringHelper.isNotEmpty( startElement.getName().getNamespaceURI() );
	}

	protected <X extends T> X jaxb(XMLEventReader reader, Schema xsd, JAXBContext jaxbContext, Origin origin) {
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();

		try {
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( xsd );
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
