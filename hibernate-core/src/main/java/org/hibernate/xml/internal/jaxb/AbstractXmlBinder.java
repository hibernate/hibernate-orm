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
 * Base implementation (template) of the XmlBinder contract.
 * <p/>
 * Generally implementors should just have to implement:<ol>
 *     <li>{@link #getJaxbContext}</li>
 *     <li>{@link #getSchema}</li>
 *     <li>(optionally) {@link #wrapReader}</li>
 * </ol>
 *
 * @author Steve Ebersole
 * @author Strong Liu <stliu@hibernate.org>
 *
 * @deprecated See {@link AbstractUnifiedBinder}
 */
@Deprecated
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


		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			final Schema schema = getSchema( rootElementStartEvent, origin );
			staxEventReader = wrapReader( staxEventReader, rootElementStartEvent );

			final JAXBContext jaxbContext = getJaxbContext( rootElementStartEvent );
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( schema );
			unmarshaller.setEventHandler( handler );

			final Object target = unmarshaller.unmarshal( staxEventReader );
			return new BindResult( target, origin );
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

	/**
	 * Identify the Schema to use to validate the document being processed.
	 *
	 * @param rootElementStartEvent The peeked event that represents the start of the root element of the document
	 * @param origin
	 *
	 * @return
	 *
	 * @throws JAXBException
	 */
	protected abstract Schema getSchema(XMLEvent rootElementStartEvent, Origin origin) throws JAXBException;

	/**
	 * Wrap the given StAX event reader in another if needed.
	 *
	 * @param xmlEventReader The "real" reader.
	 * @param rootElementStartEvent The peeked event that represents the start of the root element of the document
	 *
	 * @return The wrapped reader.  Simply return the given reader if no wrapping is needed.
	 */
	protected XMLEventReader wrapReader(XMLEventReader xmlEventReader, XMLEvent rootElementStartEvent) {
		return xmlEventReader;
	}

	/**
	 * Get the JAXB context representing the Java model to bind to.
	 *
	 * @param event
	 *
	 * @return
	 *
	 * @throws JAXBException
	 */
	protected abstract JAXBContext getJaxbContext(XMLEvent event) throws JAXBException;

	protected static boolean isNamespaced(StartElement startElement) {
		return ! "".equals( startElement.getName().getNamespaceURI() );
	}

}
