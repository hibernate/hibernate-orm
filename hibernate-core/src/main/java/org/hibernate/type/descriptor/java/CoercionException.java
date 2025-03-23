/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.HibernateException;

/**
 * A problem converting between JDBC types and Java types.
 *
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
