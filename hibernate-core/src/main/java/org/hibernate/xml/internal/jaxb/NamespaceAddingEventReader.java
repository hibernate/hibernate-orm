/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

/**
 * Used to wrap a StAX {@link XMLEventReader} in order to introduce namespaces into the underlying document.  This
 * is intended for temporary migration feature to allow legacy HBM mapping documents (DTD-based) to continue to
 * parse correctly.  This feature will go away eventually.
 *
 * @author Steve Ebersole
 */
public class NamespaceAddingEventReader extends EventReaderDelegate {
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
		final List<Namespace> namespaces = new ArrayList<Namespace>();
		namespaces.add( xmlEventFactory.createNamespace( "", namespaceUri ) );
		Iterator<?> originalNamespaces = startElement.getNamespaces();
		while ( originalNamespaces.hasNext() ) {
			Namespace ns = (Namespace) originalNamespaces.next();
			namespaces.add( ns );
		}
		return xmlEventFactory.createStartElement(
				new QName( namespaceUri, startElement.getName().getLocalPart() ),
				startElement.getAttributes(),
				namespaces.iterator()
		);
	}

	@Override
	public XMLEvent nextEvent() throws XMLStreamException {
		return wrap( super.nextEvent() );
	}

	private XMLEvent wrap(XMLEvent event) {
		if ( event.isStartElement() ) {
			return withNamespace( event.asStartElement() );
		}
		return event;
	}

	@Override
	public XMLEvent peek() throws XMLStreamException {
		return wrap( super.peek() );
	}
}
