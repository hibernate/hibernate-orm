/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.xml.internal.jaxb;

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

import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.xml.internal.stax.BufferedXMLEventReader;
import org.hibernate.xml.internal.stax.LocalXmlResourceResolver;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.UnifiedBinder;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractUnifiedBinder<T> implements UnifiedBinder<T> {
	private final boolean validateXml;

	protected AbstractUnifiedBinder() {
		this( true );
	}

	protected AbstractUnifiedBinder(boolean validateXml) {
		this.validateXml = validateXml;
	}

	public boolean isValidationEnabled() {
		return validateXml;
	}

	@Override
	public T bind(InputStream stream, Origin origin) {
		final XMLEventReader eventReader = createReader( stream, origin );
		return doBind( eventReader, origin );
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
	public T bind(Source source, Origin origin) {
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

	private T doBind(XMLEventReader eventReader, Origin origin) {
		try {
			final StartElement rootElementStartEvent = seekRootElementStartEvent( eventReader, origin );
			return doBind( eventReader, rootElementStartEvent, origin );
		}
		finally {
			try {
				eventReader.close();
			}
			catch ( Exception ignore ) {
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
		staxFactory.setXMLResolver( LocalXmlResourceResolver.INSTANCE );
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

	protected abstract T doBind(XMLEventReader staxEventReader, StartElement rootElementStartEvent, Origin origin);

	protected static boolean hasNamespace(StartElement startElement) {
		return ! "".equals( startElement.getName().getNamespaceURI() );
	}

	@SuppressWarnings("unchecked")
	protected <T> T jaxb(XMLEventReader reader, Schema xsd, Class<T> modelClass, Origin origin) {
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();

		try {
			final JAXBContext jaxbContext = JAXBContext.newInstance( modelClass );
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
