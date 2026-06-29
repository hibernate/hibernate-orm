/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.io.Serializable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	public AbstractSessionEvent(@Nullable EventSource source) {
		this.source = source;
	}

	/**
	 * Returns the session event source for this event. This is the underlying
	 * session from which this event was generated.
	 *
	 * @return The session event source.
	 */
	@Nonnull
	public final EventSource getSession() {
		return getEventSource();
	}

	@Nonnull
	public final EventSource getEventSource() {
		if ( source == null ) {
			throw new IllegalStateException( "EventSource not available" );
		}
		return source.asEventSource();
	}

	@Nonnull
	public SessionFactoryImplementor getFactory() {
		if ( source == null ) {
			throw new IllegalStateException( "EventSource not available" );
		}
		return source.getFactory();
	}
}
