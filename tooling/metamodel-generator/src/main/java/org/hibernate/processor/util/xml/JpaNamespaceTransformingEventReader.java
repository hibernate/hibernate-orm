/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util.xml;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Transforms the version attribute and namespace of the JPA configuration files (persistence.xml and orm.xml) to
 * the default. For the purpose of the metamodel it is enough to parse the xml against the latest specification version
 * and schema.
 *
 * @author Hardy Ferentschik
 */
public class JpaNamespaceTransformingEventReader extends EventReaderDelegate {
	private static final String VERSION_ATTRIBUTE_NAME = "version";
	private static final String DEFAULT_PERSISTENCE_VERSION = "3.0";
	private static final String DEFAULT_ORM_VERSION = "3.1";

	private static final String DEFAULT_PERSISTENCE_NAMESPACE = "https://jakarta.ee/xml/ns/persistence";
	private static final String DEFAULT_ORM_NAMESPACE = "https://jakarta.ee/xml/ns/persistence/orm";
	private static final Map<String, String> NAMESPACE_MAPPING = new HashMap<String, String>( 8 );

	static {
		NAMESPACE_MAPPING.put(
				"http://java.sun.com/xml/ns/persistence",
				DEFAULT_PERSISTENCE_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"http://java.sun.com/xml/ns/persistence/orm",
				DEFAULT_ORM_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"http://xmlns.jcp.org/xml/ns/persistence",
				DEFAULT_PERSISTENCE_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"http://xmlns.jcp.org/xml/ns/persistence/orm",
				DEFAULT_ORM_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"https://java.sun.com/xml/ns/persistence",
				DEFAULT_PERSISTENCE_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"https://java.sun.com/xml/ns/persistence/orm",
				DEFAULT_ORM_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"https://xmlns.jcp.org/xml/ns/persistence",
				DEFAULT_PERSISTENCE_NAMESPACE
		);
		NAMESPACE_MAPPING.put(
				"https://xmlns.jcp.org/xml/ns/persistence/orm",
				DEFAULT_ORM_NAMESPACE
		);
	}

	private static final Map<String, String> START_ELEMENT_TO_NAMESPACE_URI = new HashMap<String, String>( 2 );

	static {
		START_ELEMENT_TO_NAMESPACE_URI.put(
				"persistence",
				DEFAULT_PERSISTENCE_NAMESPACE
		);
		START_ELEMENT_TO_NAMESPACE_URI.put(
				"entity-mappings",
				DEFAULT_ORM_NAMESPACE
		);
	}

	private static final String EMPTY_PREFIX = "";

	private final XMLEventFactory xmlEventFactory;
	private @Nullable String currentDocumentNamespaceUri;

	public JpaNamespaceTransformingEventReader(XMLEventReader reader) {
		super( reader );
		this.xmlEventFactory = XMLEventFactory.newInstance();
	}

	@Override
	public @Nullable XMLEvent peek() throws XMLStreamException {
		return wrap( super.peek() );
	}

	@Override
	public @Nullable XMLEvent nextEvent() throws XMLStreamException {
		return wrap( super.nextEvent() );
	}

	private @Nullable XMLEvent wrap(@Nullable XMLEvent event) {
		if ( event != null && event.isStartElement() ) {
			return transform( event.asStartElement() );
		}
		return event;
	}

	private StartElement transform(StartElement startElement) {
		String elementName = startElement.getName().getLocalPart();
		// use the start element to determine whether we have a persistence.xml or orm.xml
		if ( START_ELEMENT_TO_NAMESPACE_URI.containsKey( elementName ) ) {
			currentDocumentNamespaceUri = START_ELEMENT_TO_NAMESPACE_URI.get( elementName );
		}

		List<Attribute> newElementAttributeList = updateElementAttributes( startElement );
		List<Namespace> newNamespaceList = updateElementNamespaces( startElement );

		// create the new element
		return xmlEventFactory.createStartElement(
				new QName( currentDocumentNamespaceUri, startElement.getName().getLocalPart() ),
				newElementAttributeList.iterator(),
				newNamespaceList.iterator()
		);
	}

	private List<Namespace> updateElementNamespaces(StartElement startElement) {
		List<Namespace> newNamespaceList = new ArrayList<>();
		Iterator<Namespace> existingNamespaceIterator = startElement.getNamespaces();
		while ( existingNamespaceIterator.hasNext() ) {
			Namespace namespace = existingNamespaceIterator.next();
			if ( NAMESPACE_MAPPING.containsKey( namespace.getNamespaceURI() ) ) {
				newNamespaceList.add( xmlEventFactory.createNamespace( EMPTY_PREFIX, currentDocumentNamespaceUri ) );
			}
			else {
				newNamespaceList.add( namespace );
			}
		}

		// if there is no namespace at all we add the main one. All elements need the namespace
		if ( newNamespaceList.isEmpty() ) {
			newNamespaceList.add( xmlEventFactory.createNamespace( EMPTY_PREFIX, currentDocumentNamespaceUri ) );
		}

		return newNamespaceList;
	}

	private List<Attribute> updateElementAttributes(StartElement startElement) {
		// adjust the version attribute
		List<Attribute> newElementAttributeList = new ArrayList<>();
		Iterator<Attribute> existingAttributesIterator = startElement.getAttributes();
		while ( existingAttributesIterator.hasNext() ) {
			Attribute attribute = existingAttributesIterator.next();
			if ( VERSION_ATTRIBUTE_NAME.equals( attribute.getName().getLocalPart() ) ) {
				if ( currentDocumentNamespaceUri != null &&
					currentDocumentNamespaceUri.equals( DEFAULT_PERSISTENCE_NAMESPACE ) ) {
					if ( !DEFAULT_PERSISTENCE_VERSION.equals( attribute.getName().getPrefix() ) ) {
						newElementAttributeList.add(
								xmlEventFactory.createAttribute(
										attribute.getName(),
										DEFAULT_PERSISTENCE_VERSION
								)
						);
					}
				}
				else {
					if ( !DEFAULT_ORM_VERSION.equals( attribute.getName().getPrefix() ) ) {
						newElementAttributeList.add(
								xmlEventFactory.createAttribute(
										attribute.getName(),
										DEFAULT_ORM_VERSION
								)
						);
					}
				}
			}
			else {
				newElementAttributeList.add( attribute );
			}
		}
		return newElementAttributeList;
	}
}
