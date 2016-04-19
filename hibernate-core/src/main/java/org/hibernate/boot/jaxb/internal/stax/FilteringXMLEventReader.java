/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Base class for {@link javax.xml.stream.XMLEventReader}s that want to modify or remove events from the reader stream.
 * If a {@link javax.xml.stream.events.StartElement} event is removed the subclass's {@link #filterEvent(javax.xml.stream.events.XMLEvent, boolean)} will
 * not see any events until afterQuery the matching {@link javax.xml.stream.events.EndElement} event.
 *
 * Note, copied from the uPortal project by permission of author.  See
 * https://github.com/Jasig/uPortal/blob/master/uportal-war/src/main/java/org/jasig/portal/xml/stream/FilteringXMLEventReader.java
 *
 * @author Eric Dalquist
 */
public abstract class FilteringXMLEventReader extends BaseXMLEventReader {
	private final Deque<QName> prunedElements = new LinkedList<QName>();
	private XMLEvent peekedEvent;

	public FilteringXMLEventReader(XMLEventReader reader) {
		super(reader);
	}

	@Override
	protected final XMLEvent internalNextEvent() throws XMLStreamException {
		return this.internalNext(false);
	}

	@Override
	public boolean hasNext() {
		try {
			return peekedEvent != null || (super.hasNext() && this.peek() != null);
		}
		catch (XMLStreamException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		catch (NoSuchElementException e) {
			return false;
		}
	}

	@Override
	public final XMLEvent peek() throws XMLStreamException {
		if (peekedEvent != null) {
			return peekedEvent;
		}

		peekedEvent = internalNext(true);
		return peekedEvent;
	}

	protected final XMLEvent internalNext(boolean peek) throws XMLStreamException {
		XMLEvent event = null;

		if (peekedEvent != null) {
			event = peekedEvent;
			peekedEvent = null;
			return event;
		}

		do {
			event = super.getParent().nextEvent();

			//If there are pruned elements in the queue filtering events is still needed
			if (!prunedElements.isEmpty()) {
				//If another start element add it to the queue
				if (event.isStartElement()) {
					final StartElement startElement = event.asStartElement();
					prunedElements.push(startElement.getName());
				}
				//If end element pop the newest name of the queue and double check that the start/end elements match up
				else if (event.isEndElement()) {
					final QName startElementName = prunedElements.pop();

					final EndElement endElement = event.asEndElement();
					final QName endElementName = endElement.getName();

					if (!startElementName.equals(endElementName)) {
						throw new IllegalArgumentException("Malformed XMLEvent stream. Expected end element for " + startElementName + " but found end element for " + endElementName);
					}
				}

				event = null;
			}
			else {
				final XMLEvent filteredEvent = this.filterEvent(event, peek);

				//If the event is being removed and it is a start element all elements until the matching
				//end element need to be removed as well
				if (filteredEvent == null && event.isStartElement()) {
					final StartElement startElement = event.asStartElement();
					final QName name = startElement.getName();
					prunedElements.push(name);
				}

				event = filteredEvent;
			}
		}
		while (event == null);

		return event;
	}

	/**
	 * @param event The current event
	 * @param peek If the event is from a {@link #peek()} call
	 * @return The event to return, if null is returned the event is dropped from the stream and the next event will be used.
	 */
	protected abstract XMLEvent filterEvent(XMLEvent event, boolean peek);
}
