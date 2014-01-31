/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.validation.Schema;

import org.jboss.logging.Logger;

import org.hibernate.xml.internal.stax.BufferedXMLEventReader;
import org.hibernate.xml.internal.stax.LocalXmlResourceResolver;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.xml.spi.XmlBinder;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
abstract class AbstractXmlBinder implements XmlBinder {
	protected static final Logger log = Logger.getLogger( AbstractXmlBinder.class );

	protected final ServiceRegistry serviceRegistry;
	protected final boolean validateXml;

	public AbstractXmlBinder(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, true );
	}

	public AbstractXmlBinder(ServiceRegistry serviceRegistry, boolean validateXml) {
		this.serviceRegistry = serviceRegistry;
		this.validateXml = validateXml;
	}

	@Override
	public BindResult bind(InputStream stream, Origin origin) {
		try {
			BufferedXMLEventReader staxReader = new BufferedXMLEventReader(staxFactory().createXMLEventReader( stream ), 100);
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
		staxFactory.setXMLResolver( LocalXmlResourceResolver.INSTANCE );
		return staxFactory;
	}

	@SuppressWarnings( { "unchecked" })
	private BindResult unmarshal(XMLEventReader staxEventReader, final Origin origin) {
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



		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			Schema schema = getSchema(event, origin);
			staxEventReader = wrapReader( staxEventReader, event );
			JAXBContext jaxbContext =getJaxbContext(event);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( schema );
			unmarshaller.setEventHandler( handler );
			final Object target = unmarshaller.unmarshal( staxEventReader );
			return new BindResult( target, origin );
		}
		catch ( JAXBException e ) {
			throw new MappingException(
					"Unable to perform unmarshalling at line number " + handler.getLineNumber()
							+ " and column " + handler.getColumnNumber()
							+ ". Message: " + handler.getMessage(), e, origin
			);
		}
	}
	protected abstract JAXBContext getJaxbContext(XMLEvent event) throws JAXBException;
	protected abstract Schema getSchema(XMLEvent event, Origin origin) throws JAXBException;
	protected XMLEventReader wrapReader(XMLEventReader xmlEventReader, XMLEvent event){
		return xmlEventReader;
	}
	protected static boolean isNamespaced(StartElement startElement) {
		return ! "".equals( startElement.getName().getNamespaceURI() );
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
