/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jndi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem with a given JNDI name being deemed as not valid.
 *
 * @author Steve Ebersole
 */
public class JndiNameException extends HibernateException {
	/**
	 * Constructs a JndiNameException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause.
	 */
	public JndiNameException(String message, Throwable cause) {
		super( message, cause );
	}
}
