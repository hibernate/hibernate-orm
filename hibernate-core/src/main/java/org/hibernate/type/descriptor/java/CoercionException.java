/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class CoercionException extends HibernateException {
	public CoercionException(String message) {
		super( message );
	}

	public CoercionException(String message, Throwable cause) {
		super( message, cause );
	}
}
