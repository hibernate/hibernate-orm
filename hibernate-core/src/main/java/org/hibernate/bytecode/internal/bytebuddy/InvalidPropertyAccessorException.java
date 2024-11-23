/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.HibernateException;

public class InvalidPropertyAccessorException extends HibernateException {

	public InvalidPropertyAccessorException(String message) {
		super( message );
	}
}
