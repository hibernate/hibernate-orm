/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.HibernateException;

/**
 * An attribute required, but it is missing.
 * There are a number of possible underlying causes, including:
 * <ul>
 * <li>Missing annotation attribute,
 * <li>Missing configuration
 * </ul>
 *
 * @author Bao Ngo
 */
public class MissingAttributeException extends HibernateException {

	/**
	 * Constructs a {@code MissingAttributeException} using the given exception message.
	 *
	 * @param message The message explaining the reason for the exception
	 */
	public MissingAttributeException(String message) {
		super( message );
	}
}
