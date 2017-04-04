/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem while building a PropertyAccess
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
