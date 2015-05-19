/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction;

import org.hibernate.HibernateException;

/**
 * Wraps an exception thrown from a "local synchronization" (one registered in the SynchronizationRegistry).
 *
 * @author Steve Ebersole
 */
public class LocalSynchronizationException extends HibernateException {
	public LocalSynchronizationException(String message, Throwable cause) {
		super( message, cause );
	}
}
