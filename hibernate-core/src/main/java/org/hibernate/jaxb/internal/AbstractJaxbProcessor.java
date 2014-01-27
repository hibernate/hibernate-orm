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
package org.hibernate.jaxb.internal;

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

import org.hibernate.internal.util.xml.BufferedXMLEventReader;
import org.hibernate.internal.util.xml.LocalXmlResourceResolver;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
abstract class AbstractJaxbProcessor {
	protected static final Logger log = Logger.getLogger( AbstractJaxbProcessor.class );

//	public static final String VALIDATE_XML_SETTING = "hibernate.xml.validate";

	protected final ServiceRegistry serviceRegistry;
	protected final boolean validateXml;

	public AbstractJaxbProcessor(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, true );
//		this(
//				serviceRegistry,
//				serviceRegistry.getService( ConfigurationService.class ).getSetting(
//						VALIDATE_XML_SETTING,
//						StandardConverters.BOOLEAN,
//						true
//				)
//		);
	}

	public AbstractJaxbProcessor(ServiceRegistry serviceRegistry, boolean validateXml) {
		this.serviceRegistry = serviceRegistry;
		this.validateXml = validateXml;
	}

	public JaxbRoot unmarshal(InputStream stream, Origin origin) {
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



		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			Schema schema = getSchema(event, origin);
			staxEventReader = wrapReader( staxEventReader, event );
			JAXBContext jaxbContext =getJaxbContext(event);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( schema );
			unmarshaller.setEventHandler( handler );
			final Object target = unmarshaller.unmarshal( staxEventReader );
			return new JaxbRoot( target, origin );
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
