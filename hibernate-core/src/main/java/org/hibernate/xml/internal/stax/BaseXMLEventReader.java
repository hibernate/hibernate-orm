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
package org.hibernate.xml.internal.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Base for XMLEventReader that implements the {@link #getElementText()} and {@link #nextTag()} APIs in a
 * way that is agnostic from the rest of the XMLEventReader implementation. Both will use the subclasses
 * {@link #internalNextEvent()} as the exclusive way to read events.
 *
 * Note, copied from the uPortal project by permission of author.  See
 * https://github.com/Jasig/uPortal/blob/master/uportal-war/src/main/java/org/jasig/portal/xml/stream/BaseXMLEventReader.java
 *
 * @author Eric Dalquist
 */
public abstract class BaseXMLEventReader extends EventReaderDelegate {
	private XMLEvent previousEvent;

	public BaseXMLEventReader(XMLEventReader reader) {
		super(reader);
	}

	/**
	 * Subclass's version of {@link #nextEvent()}, called by {@link #next()}
	 */
	protected abstract XMLEvent internalNextEvent() throws XMLStreamException;

	/**
	 * @return The XMLEvent returned by the last call to {@link #internalNextEvent()}
	 */
	protected final XMLEvent getPreviousEvent() {
		return this.previousEvent;
	}

	@Override
	public final XMLEvent nextEvent() throws XMLStreamException {
		this.previousEvent = this.internalNextEvent();
		return this.previousEvent;
	}

	@Override
	public final Object next() {
		try {
			return this.nextEvent();
		}
		catch (XMLStreamException e) {
			return null;
		}
	}

	@Override
	public final String getElementText() throws XMLStreamException {
		XMLEvent event = this.previousEvent;
		if (event == null) {
			throw new XMLStreamException("Must be on START_ELEMENT to read next text, element was null");
		}
		if (!event.isStartElement()) {
			throw new XMLStreamException("Must be on START_ELEMENT to read next text", event.getLocation());
		}

		final StringBuilder text = new StringBuilder();
		while (!event.isEndDocument()) {
			switch (event.getEventType()) {
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.SPACE:
				case XMLStreamConstants.CDATA: {
					final Characters characters = event.asCharacters();
					text.append(characters.getData());
					break;
				}
				case XMLStreamConstants.ENTITY_REFERENCE: {
					final EntityReference entityReference = (EntityReference)event;
					final EntityDeclaration declaration = entityReference.getDeclaration();
					text.append(declaration.getReplacementText());
					break;
				}
				case XMLStreamConstants.COMMENT:
				case XMLStreamConstants.PROCESSING_INSTRUCTION: {
					//Ignore
					break;
				}
				default: {
					throw new XMLStreamException("Unexpected event type '" + XMLStreamConstantsUtils.getEventName(event.getEventType()) + "' encountered. Found event: " + event, event.getLocation());
				}
			}

			event = this.nextEvent();
		}

		return text.toString();
	}

	@Override
	public final XMLEvent nextTag() throws XMLStreamException {
		XMLEvent event = this.nextEvent();
		while ((event.isCharacters() && event.asCharacters().isWhiteSpace())
				|| event.isProcessingInstruction()
				|| event.getEventType() == XMLStreamConstants.COMMENT) {

			event = this.nextEvent();
		}

		if (!event.isStartElement()  && event.isEndElement()) {
			throw new XMLStreamException("Unexpected event type '" + XMLStreamConstantsUtils.getEventName(event.getEventType()) + "' encountered. Found event: " + event, event.getLocation());
		}

		return event;
	}
}
