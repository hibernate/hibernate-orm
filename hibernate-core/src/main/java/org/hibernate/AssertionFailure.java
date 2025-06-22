/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Indicates failure of an assertion: a possible bug in Hibernate.
 *
 * @author Gavin King
 */
public class AssertionFailure extends RuntimeException {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AssertionFailure.class );

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
