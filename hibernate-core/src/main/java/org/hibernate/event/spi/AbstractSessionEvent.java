/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.io.Serializable;

/**
 * Base class for events which are generated from a {@linkplain org.hibernate.Session Session}
 * ({@linkplain EventSource}).
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSessionEvent implements Serializable {
	protected final EventSource source;

	/**
	 * Constructs an event from the given event session.
	 *
	 * @param source The session event source.
	 */
	public AbstractSessionEvent(EventSource source) {
		this.source = source;
	}

	/**
	 * Returns the session event source for this event. This is the underlying
	 * session from which this event was generated.
	 *
	 * @return The session event source.
	 */
	public final EventSource getSession() {
		return getEventSource();
	}

	public final EventSource getEventSource() {
		return source.asEventSource();
	}

	public SessionFactoryImplementor getFactory() {
		return source.getFactory();
	}
}
