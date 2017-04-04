/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
