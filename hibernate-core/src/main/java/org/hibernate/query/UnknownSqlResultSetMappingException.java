/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.MappingException;

import jakarta.persistence.NamedNativeQuery;

/**
 * Indicates a request for named ResultSet mapping which could not be found
 *
 * @see NamedNativeQuery#resultSetMapping()
 * @see org.hibernate.Session#createNativeQuery(String, String)
 * @see org.hibernate.Session#createNativeQuery(String, String, Class)
 * @see org.hibernate.Session#getNamedNativeQuery(String, String)
 *
 * @author Steve Ebersole
 */
public class UnknownSqlResultSetMappingException extends MappingException {
	private final String unknownSqlResultSetMappingName;

	public UnknownSqlResultSetMappingException(String unknownSqlResultSetMappingName) {
		super( "The given SqlResultSetMapping name [" + unknownSqlResultSetMappingName + "] is unknown" );
		this.unknownSqlResultSetMappingName = unknownSqlResultSetMappingName;
	}

	public String getUnknownSqlResultSetMappingName() {
		return unknownSqlResultSetMappingName;
	}
}
