/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jndi;
import org.hibernate.HibernateException;

/**
 * An exception indicating trouble accessing JNDI
 *
 * @author Steve Ebersole
 */
public class JndiException extends HibernateException {
	/**
	 * Constructs a JndiException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public JndiException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a JndiException
	 *
	 * @param message Message explaining the exception condition
	 */
	public JndiException(String message) {
		super( message );
	}

}
