/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.HibernateException;

public class InvalidPropertyAccessorException extends HibernateException {

	public InvalidPropertyAccessorException(String message) {
		super( message );
	}
}
