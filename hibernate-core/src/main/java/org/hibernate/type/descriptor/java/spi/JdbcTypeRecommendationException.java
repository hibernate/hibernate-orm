/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Exception indicating {@link JavaType#getRecommendedJdbcType} could not
 * determine a recommended JDBC type descriptor
 *
 * @author Steve Ebersole
 */
public class JdbcTypeRecommendationException extends HibernateException {
	public JdbcTypeRecommendationException(String message) {
		super( message );
	}

	public JdbcTypeRecommendationException(String message, Throwable cause) {
		super( message, cause );
	}
}
