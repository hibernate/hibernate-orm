/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
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
