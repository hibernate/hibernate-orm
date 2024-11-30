/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem while building a {@link PropertyAccess}.
 *
 * @author Steve Ebersole
 */
public class PropertyAccessBuildingException extends HibernateException {
	public PropertyAccessBuildingException(String message) {
		super( message );
	}

	public PropertyAccessBuildingException(String message, Throwable cause) {
		super( message, cause );
	}
}
