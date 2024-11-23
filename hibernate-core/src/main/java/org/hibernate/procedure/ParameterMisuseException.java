/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * Thrown to indicate a misuse of a parameter
 *
 * @author Steve Ebersole
 */
public class ParameterMisuseException extends HibernateException {
	public ParameterMisuseException(String message) {
		super( message );
	}
}
