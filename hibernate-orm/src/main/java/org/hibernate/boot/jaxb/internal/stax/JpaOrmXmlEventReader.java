/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * A JPA {@code orm.xml} specific StAX EVentReader to handle a few oddities.
 *
 * Mainly we handle the namespace change.
 *
 * Ultimately we should handle "upgrading" the documents as well.  The idea being that
 * we'd always treat all versions as the latest.
 *
 * {@see HHH-8108} for more discussion.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class JpaOrmXmlEventReader extends EventReaderDelegate {
	private static final List<String> NAMESPACE_URIS_TO_MAP = Arrays.asList(
			// JPA 1.0 and 2.0 namespace uri
			"http://java.sun.com/xml/ns/persistence/orm"
	);

	private static final String ROOT_ELEMENT_NAME = "entity-mappings";
	private static final String VERSION_ATTRIBUTE_NAME = "version";

	private static final String DEFAULT_VERSION = "2.1";
	private static final List<String> VALID_VERSIONS = Arrays.asList( "1.0", "2.0", "2.1" );

	private final XMLEventFactory xmlEventFactory;

	public JpaOrmXmlEventReader(XMLEventReader reader) {
		this( reader, XMLEventFactory.newInstance() );
	}

	public JpaOrmXmlEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory) {
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
		if ( event != null ) {
			if ( event.isStartElement() ) {
				return wrap( event.asStartElement() );
			}
			else if ( event.isEndElement() ) {
				return wrap( event.asEndElement() );
			}
		}
		return event;
	}

	private StartElement wrap(StartElement startElement) {
		List<Attribute> newElementAttributeList = mapAttributes( startElement );
		List<Namespace> newNamespaceList = mapNamespaces( startElement );

		// Transfer the location info from the incoming event to the event factory
		// so that the event we ask it to generate for us has the same location info
		xmlEventFactory.setLocation( startElement.getLocation() );
		return xmlEventFactory.createStartElement(
				new QName( LocalSchema.ORM.getNamespaceUri(), startElement.getName().getLocalPart() ),
				newElementAttributeList.iterator(),
				newNamespaceList.iterator()
		);
	}

	private List<Attribute> mapAttributes(StartElement startElement) {
		final List<Attribute> mappedAttributes = new ArrayList<Attribute>();

		Iterator<Attribute> existingAttributesIterator = existingXmlAttributesIterator( startElement );
		while ( existingAttributesIterator.hasNext() ) {
			final Attribute originalAttribute = existingAttributesIterator.next();
			final Attribute attributeToUse = mapAttribute( startElement, originalAttribute );
			mappedAttributes.add( attributeToUse );
		}

		return mappedAttributes;
	}

	@SuppressWarnings("unchecked")
	private Iterator<Attribute> existingXmlAttributesIterator(StartElement startElement) {
		return startElement.getAttributes();
	}

	private Attribute mapAttribute(StartElement startElement, Attribute originalAttribute) {
		// Here we look to see if this attribute is the JPA version attribute, and if so do 2 things:
		//		1) validate its version attribute is valid
		//		2) update its version attribute to the default version if not already
		//
		// NOTE : atm this is a very simple check using just the attribute's local name
		// rather than checking its qualified name.  It is possibly (though unlikely)
		// that this could match on "other" version attributes in the same element

		if ( ROOT_ELEMENT_NAME.equals( startElement.getName().getLocalPart() ) ) {
			if ( VERSION_ATTRIBUTE_NAME.equals( originalAttribute.getName().getLocalPart() ) ) {
				final String specifiedVersion = originalAttribute.getValue();

				if ( !VALID_VERSIONS.contains( specifiedVersion ) ) {
					throw new BadVersionException( specifiedVersion );
				}

				return xmlEventFactory.createAttribute( VERSION_ATTRIBUTE_NAME, DEFAULT_VERSION );
			}
		}

		return originalAttribute;
	}

	private List<Namespace> mapNamespaces(StartElement startElement) {
		return mapNamespaces( existingXmlNamespacesIterator( startElement ) );
	}

	private List<Namespace> mapNamespaces(Iterator<Namespace> originalNamespaceIterator ) {
		final List<Namespace> mappedNamespaces = new ArrayList<Namespace>();

//		final String elementNamespacePrefix = startElement.getName().getPrefix();
//		if ( EMPTY_NAMESPACE_PREFIX.equals( elementNamespacePrefix ) ) {
//			// add the default namespace mapping
//			mappedNamespaces.add( xmlEventFactory.createNamespace( LocalSchema.ORM.getNamespaceUri() ) );
//		}

		while ( originalNamespaceIterator.hasNext() ) {
			final Namespace originalNamespace  = originalNamespaceIterator.next();
			final Namespace mappedNamespace = mapNamespace( originalNamespace );
			mappedNamespaces.add( mappedNamespace );
		}

		if ( mappedNamespaces.isEmpty() ) {
			mappedNamespaces.add( xmlEventFactory.createNamespace( LocalSchema.ORM.getNamespaceUri() ) );
		}

		return mappedNamespaces;
	}

	@SuppressWarnings("unchecked")
	private Iterator<Namespace> existingXmlNamespacesIterator(StartElement startElement) {
		return startElement.getNamespaces();
	}

	private Namespace mapNamespace(Namespace originalNamespace) {
		if ( NAMESPACE_URIS_TO_MAP.contains( originalNamespace.getNamespaceURI() ) ) {
			// this is a namespace "to map" so map it
			return xmlEventFactory.createNamespace( originalNamespace.getPrefix(), LocalSchema.ORM.getNamespaceUri() );
		}

		return originalNamespace;
	}

	private XMLEvent wrap(EndElement endElement) {
		final List<Namespace> targetNamespaces = mapNamespaces( existingXmlNamespacesIterator( endElement ) );

		// Transfer the location info from the incoming event to the event factory
		// so that the event we ask it to generate for us has the same location info
		xmlEventFactory.setLocation( endElement.getLocation() );
		return xmlEventFactory.createEndElement(
				new QName( LocalSchema.ORM.getNamespaceUri(), endElement.getName().getLocalPart() ),
				targetNamespaces.iterator()
		);
	}

	@SuppressWarnings("unchecked")
	private Iterator<Namespace> existingXmlNamespacesIterator(EndElement endElement) {
		return endElement.getNamespaces();
	}

	public static class BadVersionException extends RuntimeException {
		private final String requestedVersion;

		public BadVersionException(String requestedVersion) {
			this.requestedVersion = requestedVersion;
		}

		public String getRequestedVersion() {
			return requestedVersion;
		}
	}
}
