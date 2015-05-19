/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.internal;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to register a null synchronization.  Basically a glorified {@link NullPointerException}
 *
 * @author Steve Ebersole
 */
public class NullSynchronizationException extends HibernateException {
	public NullSynchronizationException() {
		this( "Synchronization to register cannot be null" );
	}

	public NullSynchronizationException(String s) {
		super( s );
	}
}
