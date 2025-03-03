/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.io.Serializable;

/**
 * Defines a base class for {@link org.hibernate.Session}-generated events.
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
	 * Returns the session event source for this event. This is the underlying
	 * session from which this event was generated.
	 *
	 * @return The session event source.
	 */
	public final EventSource getSession() {
		return session;
	}

	public SessionFactoryImplementor getFactory() {
		return session.getFactory();
	}
}
