/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import org.hibernate.HibernateException;

/**
 * An exception indicating some kind of problem performing bytecode enhancement.
 *
 * @author Steve Ebersole
 */
public class EnhancementException extends HibernateException {

	public EnhancementException(String message) {
		super( message );
	}

	/**
	 * Constructs an EnhancementException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause.
	 */
	public EnhancementException(String message, Throwable cause) {
		super( message, cause );
	}
}
