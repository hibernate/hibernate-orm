/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Acts as a stand-in for an entity identifier which is supposed to be
 * generated on insert (like an IDENTITY column) where the insert needed to
 * be delayed because we were outside a transaction when the persist
 * occurred (save currently still performs the insert).
 * <p/>
 * The stand-in is only used within the {@link org.hibernate.engine.spi.PersistenceContext}
 * in order to distinguish one instance from another; it is never injected into
 * the entity instance or returned to the client...
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class DelayedPostInsertIdentifier implements Serializable, Comparable<DelayedPostInsertIdentifier> {
	private static final AtomicLong SEQUENCE = new AtomicLong( 0 );

	private final long identifier;

	/**
	 * Constructs a DelayedPostInsertIdentifier
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
		this.identifier = value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final DelayedPostInsertIdentifier that = (DelayedPostInsertIdentifier) o;
		return identifier == that.identifier;
	}

	@Override
	public int hashCode() {
		return (int) ( identifier ^ ( identifier >>> 32 ) );
	}

	@Override
	public String toString() {
		return "<delayed:" + identifier + ">";

	}

	@Override
	public int compareTo(DelayedPostInsertIdentifier that) {
		if ( this.identifier < that.identifier ) {
			return -1;
		}
		else if ( this.identifier > that.identifier ) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
