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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import org.hibernate.xml.internal.stax.LocalSchema;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class UnifiedMappingEventReader extends EventReaderDelegate {
	private static final Logger log = Logger.getLogger( UnifiedMappingEventReader.class );

	private static final List<String> JPA_NAMESPACE_URIS = Arrays.asList(
			// JPA 1.0 and 2.0 namespace uri
			"http://java.sun.com/xml/ns/persistence/orm",
			// JPA 2.1 namespace uri
			"http://xmlns.jcp.org/xml/ns/persistence/orm"
	);

	private final XMLEventFactory xmlEventFactory;

	public UnifiedMappingEventReader(XMLEventReader reader) {
		this( reader, XMLEventFactory.newInstance() );
	}

	public UnifiedMappingEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory) {
		super( reader );
		this.xmlEventFactory = xmlEventFactory;
	}

	@Override
	public XMLEvent peek() throws XMLStreamException {
		return wrap( super.peek() );
	}

	@Override
	public XMLEvent nextEvent() throws XMLStreamException {
		return wrap( super.nextEvent() );
	}

	private XMLEvent wrap(XMLEvent event) {
		if ( event != null && event.isStartElement() ) {
			return applyNamespace( event.asStartElement() );
		}
		return event;
	}

	@SuppressWarnings("unchecked")
	private StartElement applyNamespace(StartElement startElement) {
		Iterator<?> attributesItr;
		Iterator<?> namespacesItr;

		if ( "entity-mappings".equals( startElement.getName().getLocalPart() ) ) {
			final List<Namespace> targetNamespaces = new ArrayList<Namespace>();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// namespaces are a little more complicated.  We copy over all namespaces,
			// but if any uris match known JPA namespaces which point that given
			// prefix to our namespace instead.
			final Iterator<Namespace> originalNamespaces = startElement.getNamespaces();
			while ( originalNamespaces.hasNext() ) {
				Namespace namespace = originalNamespaces.next();
				if ( JPA_NAMESPACE_URIS.contains( namespace.getNamespaceURI() ) ) {
					// this namespace was recognized as a "JPA namespace" by uri,
					// so map our unified namespace uri to that prefix
					namespace = xmlEventFactory.createNamespace( namespace.getPrefix(), LocalSchema.MAPPING.getNamespaceUri() );
				}
				targetNamespaces.add( namespace );
			}

			attributesItr = startElement.getAttributes();
			namespacesItr = targetNamespaces.iterator();
		}
		else {
			attributesItr = startElement.getAttributes();
			namespacesItr = startElement.getNamespaces();
		}

		final StartElement adjusted = xmlEventFactory.createStartElement(
				new QName( LocalSchema.MAPPING.getNamespaceUri(), startElement.getName().getLocalPart() ),
				attributesItr,
				namespacesItr
		);
		if ( log.isDebugEnabled() ) {
			log.debugf( "Created new StartElement with adjusted namespace : %s ", adjusted );
		}
		return adjusted;
	}
}
