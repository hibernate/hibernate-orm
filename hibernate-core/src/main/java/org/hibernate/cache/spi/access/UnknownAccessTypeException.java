/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.access;

import org.hibernate.HibernateException;

/**
 * Indicates that an unknown AccessType external name was encountered
 *
 * @author Steve Ebersole
 *
 * @see AccessType#fromExternalName(String)
 */
public class UnknownAccessTypeException extends HibernateException {
	/**
	 * Constructs the UnknownAccessTypeException.
	 *
	 * @param accessTypeName The external name that could not be resolved.
	 */
	public UnknownAccessTypeException(String accessTypeName) {
		super( "Unknown access type [" + accessTypeName + "]" );
	}
}
