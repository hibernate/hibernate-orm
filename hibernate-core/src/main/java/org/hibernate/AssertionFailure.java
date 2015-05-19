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
 * Indicates failure of an assertion: a possible bug in Hibernate.
 *
 * @author Gavin King
 */
public class AssertionFailure extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AssertionFailure.class.getName()
	);

	/**
	 * Creates an instance of AssertionFailure using the given message.
	 *
	 * @param message The message explaining the reason for the exception
	 */
	public AssertionFailure(String message) {
		super( message );
		LOG.failed( this );
	}

	/**
	 * Creates an instance of AssertionFailure using the given message and underlying cause.
	 *
	 * @param message The message explaining the reason for the exception
	 * @param cause The underlying cause.
	 */
	public AssertionFailure(String message, Throwable cause) {
		super( message, cause );
		LOG.failed( cause );
	}
}
