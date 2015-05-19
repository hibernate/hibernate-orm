/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Indicates an attempt to access not-yet-fetched data outside of a session context.
 *
 * For example, when an uninitialized proxy or collection is accessed after the session was closed.
 *
 * @see Hibernate#initialize(java.lang.Object)
 * @see Hibernate#isInitialized(java.lang.Object)
 * @author Gavin King
 */
public class LazyInitializationException extends HibernateException {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			LazyInitializationException.class.getName()
	);

	/**
	 * Constructs a LazyInitializationException using the given message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public LazyInitializationException(String message) {
		super( message );
		LOG.trace( message, this );
	}

}
