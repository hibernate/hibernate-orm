package org.hibernate.action;

import java.io.Serializable;

/**
 * Acts as a stand-in for an entity identifier which is supposed to be
 * generated on insert (like an IDENTITY column) where the insert needed to
 * be delayed because we were outside a transaction when the persist
 * occurred (save currently still performs the insert).
 * <p/>
 * The stand-in is only used within the {@link org.hibernate.engine.PersistenceContext}
 * in order to distinguish one instance from another; it is never injected into
 * the entity instance or returned to the client...
 *
 * @author Steve Ebersole
 */
public class DelayedPostInsertIdentifier implements Serializable {
	private static long SEQUENCE = 0;
	private final long sequence;

	public DelayedPostInsertIdentifier() {
		synchronized( DelayedPostInsertIdentifier.class ) {
			if ( SEQUENCE == Long.MAX_VALUE ) {
				SEQUENCE = 0;
			}
			this.sequence = SEQUENCE++;
		}
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final DelayedPostInsertIdentifier that = ( DelayedPostInsertIdentifier ) o;
		return sequence == that.sequence;
	}

	public int hashCode() {
		return ( int ) ( sequence ^ ( sequence >>> 32 ) );
	}

	public String toString() {
		return "<delayed:" + sequence + ">";

	}
}
