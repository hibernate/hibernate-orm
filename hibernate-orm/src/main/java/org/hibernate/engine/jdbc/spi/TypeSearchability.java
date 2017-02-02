/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.DatabaseMetaData;

/**
 * Describes the searchability of a data type as reported by the JDBC driver.
 *
 * @author Steve Ebersole
 */
public enum TypeSearchability {
	/**
	 * Type is not searchable.
	 * @see java.sql.DatabaseMetaData#typePredNone
	 */
	NONE,
	/**
	 * Type is fully searchable
	 * @see java.sql.DatabaseMetaData#typeSearchable
	 */
	FULL,
	/**
	 * Type is valid only in {@code WHERE ... LIKE}
	 * @see java.sql.DatabaseMetaData#typePredChar
	 */
	CHAR,
	/**
	 * Type is supported only in {@code WHERE ... LIKE}
	 * @see java.sql.DatabaseMetaData#typePredBasic
	 */
	BASIC;

	/**
	 * Based on the code retrieved from {@link java.sql.DatabaseMetaData#getTypeInfo()} for the {@code SEARCHABLE}
	 * column, return the appropriate enum.
	 *
	 * @param code The retrieved code value.
	 *
	 * @return The corresponding enum.
	 */
	public static TypeSearchability interpret(short code) {
		switch ( code ) {
			case DatabaseMetaData.typeSearchable: {
				return FULL;
			}
			case DatabaseMetaData.typePredNone: {
				return NONE;
			}
			case DatabaseMetaData.typePredBasic: {
				return BASIC;
			}
			case DatabaseMetaData.typePredChar: {
				return CHAR;
			}
			default: {
				throw new IllegalArgumentException( "Unknown type searchability code [" + code + "] enountered" );
			}
		}
	}
}
