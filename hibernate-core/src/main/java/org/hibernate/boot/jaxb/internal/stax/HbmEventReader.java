/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import org.hibernate.boot.xsd.MappingXsdSupport;

/**
 * A StAX EventReader for {@code hbm.xml} files to add namespaces in documents
 * not containing namespaces.
 *
 * @author Steve Ebersole
 */
public class HbmEventReader extends EventReaderDelegate {

	private static final List<String> NAMESPACE_URIS_TO_MAP = Collections.singletonList(
			// we need to recognize the initial, prematurely-chosen hbm.xml xsd namespace
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
			targetNamespaces.add( xmlEventFactory.createNamespace( MappingXsdSupport.INSTANCE.hbmXsd().getNamespaceUri() ) );
		}

		// transfer any namespaces directly, unless it is in the "to map" list in which case
		// we transfer a mapped copy pointing to the new namespace
		final Iterator<Namespace> originalNamespaces = startElement.getNamespaces();
		while ( originalNamespaces.hasNext() ) {
			Namespace namespace = originalNamespaces.next();
			if ( NAMESPACE_URIS_TO_MAP.contains( namespace.getNamespaceURI() ) ) {
				// this is a namespace "to map" so map it
				namespace = xmlEventFactory.createNamespace( namespace.getPrefix(), MappingXsdSupport.INSTANCE.hbmXsd().getNamespaceUri() );
			}
			targetNamespaces.add( namespace );
		}

		// Transfer the location info from the incoming event to the event factory
		// so that the event we ask it to generate for us has the same location info
		xmlEventFactory.setLocation( startElement.getLocation() );
		return xmlEventFactory.createStartElement(
				new QName( MappingXsdSupport.INSTANCE.hbmXsd().getNamespaceUri(), startElement.getName().getLocalPart() ),
				startElement.getAttributes(),
				targetNamespaces.iterator()
		);
	}
}
