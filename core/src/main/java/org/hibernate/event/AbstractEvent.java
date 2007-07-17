//$Id: AbstractEvent.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;


/**
 * Defines a base class for Session generated events.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEvent implements Serializable {

	private final EventSource session;

    /**
     * Constructs an event from the given event session.
     *
     * @param source The session event source.
     */
	public AbstractEvent(EventSource source) {
		this.session = source;
	}

    /**
     * Returns the session event source for this event.  This is the underlying
     * session from which this event was generated.
     *
     * @return The session event source.
     */
	public final EventSource getSession() {
		return session;
	}

}
