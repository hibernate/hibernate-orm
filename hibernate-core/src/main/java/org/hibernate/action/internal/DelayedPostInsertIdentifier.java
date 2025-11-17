/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Acts as a stand-in for an entity identifier which is supposed to be
 * generated on insert (like an IDENTITY column) where the insert needed to
 * be delayed because we were outside a transaction when the persist operation
 * was called (save currently still performs the insert).
 * <p>
 * The stand-in is only used within the {@link org.hibernate.engine.spi.PersistenceContext}
 * in order to distinguish one instance from another; it is never injected into
 * the entity instance or returned to the client.
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class DelayedPostInsertIdentifier
		implements Serializable, Comparable<DelayedPostInsertIdentifier> {

	private static final AtomicLong SEQUENCE = new AtomicLong();

	private final long identifier;

	/**
	 * Constructs a {@link DelayedPostInsertIdentifier}
	 */
	public DelayedPostInsertIdentifier() {
		long value = SEQUENCE.incrementAndGet();
		if ( value < 0 ) {
			synchronized (SEQUENCE) {
				value = SEQUENCE.incrementAndGet();
				if ( value < 0 ) {
					SEQUENCE.set( 0 );
					value = 0;
				}
			}
		}
		identifier = value;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object  instanceof DelayedPostInsertIdentifier that) ) {
			return false;
		}
		else {
			return identifier == that.identifier;
		}
	}

	@Override
	public int hashCode() {
		return Long.hashCode( identifier );
	}

	@Override
	public String toString() {
		return "<delayed:" + identifier + ">";

	}

	@Override
	public int compareTo(DelayedPostInsertIdentifier that) {
		return Long.compare( this.identifier, that.identifier );
	}
}
