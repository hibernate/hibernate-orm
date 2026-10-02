package org.hibernate.boot.jaxb.internal.stax;

import java.util.ArrayList;
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

import org.hibernate.boot.xsd.XsdDescriptor;
import org.hibernate.boot.xsd.XsdHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEventReader extends EventReaderDelegate {
	private static final String VERSION_ATTRIBUTE_NAME = "version";

	private final String rootElementName;
	private final XsdDescriptor xsdDescriptor;
	private final XMLEventFactory xmlEventFactory;

	public AbstractEventReader(
			String rootElementName,
			XsdDescriptor xsdDescriptor,
			XMLEventReader reader,
			XMLEventFactory xmlEventFactory) {
		super( reader );
		this.rootElementName = rootElementName;
		this.xsdDescriptor = xsdDescriptor;
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
		final List<Attribute> newElementAttributeList = mapAttributes( startElement );
		final List<Namespace> newNamespaceList = mapNamespaces( startElement );

		// Transfer the location info from the incoming event to the event factory
		// so that the event we ask it to generate for us has the same location info
		xmlEventFactory.setLocation( startElement.getLocation() );
		return xmlEventFactory.createStartElement(
				mapName( startElement.getName() ),
				newElementAttributeList.iterator(),
				newNamespaceList.iterator()
		);
	}

	private Iterator<Attribute> existingXmlAttributesIterator(StartElement startElement) {
		return startElement.getAttributes();
	}

	private List<Attribute> mapAttributes(StartElement startElement) {
		final List<Attribute> mappedAttributes = new ArrayList<>();

		final Iterator<Attribute> existingAttributesIterator = existingXmlAttributesIterator( startElement );
		while ( existingAttributesIterator.hasNext() ) {
			final Attribute originalAttribute = existingAttributesIterator.next();
			final Attribute attributeToUse = mapAttribute( startElement, originalAttribute );
			mappedAttributes.add( attributeToUse );
		}

		return mappedAttributes;
	}

	private Attribute mapAttribute(StartElement startElement, Attribute originalAttribute) {
		// Here we look to see if this attribute is the JPA version attribute, and if so do the following:
		//		1) validate its version attribute is valid per our "latest XSD"
		//		2) update its version attribute to the latest version if not already
		//
		if ( rootElementName.equals( startElement.getName().getLocalPart() )
				&& xsdDescriptor.getNamespaceUri().equals( mapName( startElement.getName() ).getNamespaceURI() ) ) {
			if ( new QName( VERSION_ATTRIBUTE_NAME ).equals( originalAttribute.getName() ) ) {
				final String specifiedVersion = originalAttribute.getValue();

				if ( !XsdHelper.isValidJpaVersion( specifiedVersion ) ) {
					throw new BadVersionException( specifiedVersion );
				}

				return xmlEventFactory.createAttribute( VERSION_ATTRIBUTE_NAME, xsdDescriptor.getVersion() );
			}
		}

		return originalAttribute;
	}

	private List<Namespace> mapNamespaces(StartElement startElement) {
		final List<Namespace> namespaces = mapNamespaces( existingXmlNamespacesIterator( startElement ) );
		if ( startElement.getName().getNamespaceURI().isEmpty()
				&& shouldBeMappedToLatestJpaDescriptor( "" )
				&& namespaces.stream().noneMatch( namespace -> namespace.getPrefix().isEmpty() ) ) {
			namespaces.add( xmlEventFactory.createNamespace( xsdDescriptor.getNamespaceUri() ) );
		}
		return namespaces;
	}

	private List<Namespace> mapNamespaces(Iterator<Namespace> originalNamespaceIterator ) {
		final List<Namespace> mappedNamespaces = new ArrayList<>();

		while ( originalNamespaceIterator.hasNext() ) {
			final Namespace originalNamespace  = originalNamespaceIterator.next();
			final Namespace mappedNamespace = mapNamespace( originalNamespace );
			mappedNamespaces.add( mappedNamespace );
		}

		return mappedNamespaces;
	}

	private Iterator<Namespace> existingXmlNamespacesIterator(StartElement startElement) {
		return startElement.getNamespaces();
	}

	private Namespace mapNamespace(Namespace originalNamespace) {
		if ( shouldBeMappedToLatestJpaDescriptor( originalNamespace.getNamespaceURI() ) ) {
			// this is a namespace "to map" so map it
			return xmlEventFactory.createNamespace( originalNamespace.getPrefix(), xsdDescriptor.getNamespaceUri() );
		}

		return originalNamespace;
	}

	protected abstract boolean shouldBeMappedToLatestJpaDescriptor(String uri);

	private QName mapName(QName name) {
		return shouldBeMappedToLatestJpaDescriptor( name.getNamespaceURI() )
				? new QName( xsdDescriptor.getNamespaceUri(), name.getLocalPart(), name.getPrefix() )
				: name;
	}

	private XMLEvent wrap(EndElement endElement) {
		final List<Namespace> targetNamespaces = mapNamespaces( existingXmlNamespacesIterator( endElement ) );

		// Transfer the location info from the incoming event to the event factory
		// so that the event we ask it to generate for us has the same location info
		xmlEventFactory.setLocation( endElement.getLocation() );
		return xmlEventFactory.createEndElement(
				mapName( endElement.getName() ),
				targetNamespaces.iterator()
		);
	}

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
