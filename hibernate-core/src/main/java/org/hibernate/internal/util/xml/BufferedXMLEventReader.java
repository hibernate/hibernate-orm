/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Buffers XML events for later re-reading
 *
 * Note, copied from the uPortal project by permission of author.  See
 * https://github.com/Jasig/uPortal/blob/master/uportal-war/src/main/java/org/jasig/portal/xml/stream/BufferedXMLEventReader.java
 *
 * @author Eric Dalquist
 */
public class BufferedXMLEventReader extends BaseXMLEventReader {
	private final LinkedList<XMLEvent> eventBuffer = new LinkedList<XMLEvent>();
	private int eventLimit;
	private ListIterator<XMLEvent> bufferReader;

	/**
	 * Create new buffering reader, no buffering is done until {@link #mark(int)} is called.
	 */
	public BufferedXMLEventReader(XMLEventReader reader) {
		super(reader);
	}

	/**
	 * Create new buffering reader. Calls {@link #mark(int)} with the specified event limit
	 * @see #mark(int)
	 */
	public BufferedXMLEventReader(XMLEventReader reader, int eventLimit) {
		super(reader);
		this.eventLimit = eventLimit;
	}

	/**
	 * @return A copy of the current buffer
	 */
	public List<XMLEvent> getBuffer() {
		return new ArrayList<XMLEvent>(this.eventBuffer);
	}

	/* (non-Javadoc)
	 * @see org.jasig.portal.xml.stream.BaseXMLEventReader#internalNextEvent()
	 */
	@Override
	protected XMLEvent internalNextEvent() throws XMLStreamException {
		//If there is an iterator to read from reset was called, use the iterator
		//until it runs out of events.
		if (this.bufferReader != null) {
			final XMLEvent event = this.bufferReader.next();

			//If nothing left in the iterator, remove the reference and fall through to direct reading
			if (!this.bufferReader.hasNext()) {
				this.bufferReader = null;
			}

			return event;
		}

		//Get the next event from the underlying reader
		final XMLEvent event = this.getParent().nextEvent();

		//if buffering add the event
		if (this.eventLimit != 0) {
			this.eventBuffer.offer(event);

			//If limited buffer size and buffer is too big trim the buffer.
			if (this.eventLimit > 0 && this.eventBuffer.size() > this.eventLimit) {
				this.eventBuffer.poll();
			}
		}

		return event;
	}

	@Override
	public boolean hasNext() {
		return this.bufferReader != null || super.hasNext();
	}

	@Override
	public XMLEvent peek() throws XMLStreamException {
		if (this.bufferReader != null) {
			final XMLEvent event = this.bufferReader.next();
			this.bufferReader.previous(); //move the iterator back
			return event;
		}
		return super.peek();
	}

	/**
	 * Same as calling {@link #mark(int)} with -1.
	 */
	public void mark() {
		this.mark(-1);
	}

	/**
	 * Start buffering events
	 * @param eventLimit the maximum number of events to buffer. -1 will buffer all events, 0 will buffer no events.
	 */
	public void mark(int eventLimit) {
		this.eventLimit = eventLimit;

		//Buffering no events now, clear the buffer and buffered reader
		if (this.eventLimit == 0) {
			this.eventBuffer.clear();
			this.bufferReader = null;
		}
		//Buffering limited set of events, lets trim the buffer if needed
		else if (this.eventLimit > 0) {
			//If there is an iterator check its current position and calculate the new iterator start position
			int iteratorIndex = 0;
			if (this.bufferReader != null) {
				final int nextIndex = this.bufferReader.nextIndex();
				iteratorIndex = Math.max(0, nextIndex - (this.eventBuffer.size() - this.eventLimit));
			}

			//Trim the buffer until it is not larger than the limit
			while (this.eventBuffer.size() > this.eventLimit) {
				this.eventBuffer.poll();
			}

			//If there is an iterator re-create it using the newly calculated index
			if (this.bufferReader != null) {
				this.bufferReader = this.eventBuffer.listIterator(iteratorIndex);
			}
		}
	}

	/**
	 * Reset the reader to these start of the buffered events.
	 */
	public void reset() {
		if (this.eventBuffer.isEmpty()) {
			this.bufferReader = null;
		}
		else {
			this.bufferReader = this.eventBuffer.listIterator();
		}
	}

	@Override
	public void close() throws XMLStreamException {
		this.mark(0);
		super.close();
	}

	/**
	 * @return The number of events in the buffer.
	 */
	public int bufferSize() {
		return this.eventBuffer.size();
	}

	/**
	 * If reading from the buffer afterQuery a {@link #reset()} call an {@link IllegalStateException} will be thrown.
	 */
	@Override
	public void remove() {
		if (this.bufferReader != null && this.bufferReader.hasNext()) {
			throw new IllegalStateException("Cannot remove a buffered element");
		}

		super.remove();
	}
}
