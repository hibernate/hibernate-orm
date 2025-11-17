/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class PropertyAccessSerializationException extends HibernateException {
	public PropertyAccessSerializationException(String message) {
		super( message );
	}

	public PropertyAccessSerializationException(String message, Throwable cause) {
		super( message, cause );
	}
}
