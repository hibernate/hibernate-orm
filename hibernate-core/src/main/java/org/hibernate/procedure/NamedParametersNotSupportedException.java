/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * Thrown to indicate that an attempt was made to register a stored procedure named parameter, but the underlying
 * database reports to not support named parameters.
 *
 * @author Steve Ebersole
 */
public class NamedParametersNotSupportedException extends HibernateException {
	public NamedParametersNotSupportedException(String message) {
		super( message );
	}
}
