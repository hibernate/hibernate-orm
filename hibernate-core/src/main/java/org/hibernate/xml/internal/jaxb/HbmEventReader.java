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

/**
 * @author Steve Ebersole
 */
public class HbmEventReader extends EventReaderDelegate {
	private static final List<String> NAMESPACE_URIS_TO_MAP = Arrays.asList(
			// the initial (premature) hbm.xml xsd namespace
			"http://www.hibernate.org/xsd/hibernate-mapping"
	);

	private final XMLEventFactory xmlEventFactory;

	public HbmEventReader(XMLEventReader reader) {
		this( reader, XMLEventFactory.newInstance() );
	}

	public HbmEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory) {
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
		final List<Namespace> targetNamespaces = new ArrayList<Namespace>();

		if ( "".equals( startElement.getName().getNamespaceURI() ) ) {
			// add the default namespace mapping
			targetNamespaces.add( xmlEventFactory.createNamespace( LocalSchema.HBM.getNamespaceUri() ) );
		}

		// transfer any namespaces directly, unless it is in the "to map" list in which case
		// we transfer a mapped copy pointing to the new namespace
		final Iterator<Namespace> originalNamespaces = startElement.getNamespaces();
		while ( originalNamespaces.hasNext() ) {
			Namespace namespace = originalNamespaces.next();
			if ( NAMESPACE_URIS_TO_MAP.contains( namespace.getNamespaceURI() ) ) {
				// this is a namespace "to map" so map it
				namespace = xmlEventFactory.createNamespace( namespace.getPrefix(), LocalSchema.HBM.getNamespaceUri() );
			}
			targetNamespaces.add( namespace );
		}

		return xmlEventFactory.createStartElement(
				new QName( LocalSchema.HBM.getNamespaceUri(), startElement.getName().getLocalPart() ),
				startElement.getAttributes(),
				targetNamespaces.iterator()
		);
	}
}
