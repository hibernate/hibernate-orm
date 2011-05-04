/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.action.internal;

import java.io.Serializable;

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

	@Override
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

	@Override
	public int hashCode() {
		return ( int ) ( sequence ^ ( sequence >>> 32 ) );
	}

	@Override
	public String toString() {
		return "<delayed:" + sequence + ">";

	}
}
