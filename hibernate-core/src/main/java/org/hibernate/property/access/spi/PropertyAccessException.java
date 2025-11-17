/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem reading or writing value from/to a persistent property.
 *
 * @author Steve Ebersole
 */
public class PropertyAccessException extends HibernateException {
	public PropertyAccessException(String message) {
		super( message );
	}

	public PropertyAccessException(String message, Throwable cause) {
		super( message, cause );
	}
}
