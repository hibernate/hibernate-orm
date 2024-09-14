/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
