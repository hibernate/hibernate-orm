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
	 * @see DatabaseMetaData#typePredNone
	 */
	NONE,
	/**
	 * Type is fully searchable
	 * @see DatabaseMetaData#typeSearchable
	 */
	FULL,
	/**
	 * Type is valid only in {@code WHERE ... LIKE}
	 * @see DatabaseMetaData#typePredChar
	 */
	CHAR,
	/**
	 * Type is supported only in {@code WHERE ... LIKE}
	 * @see DatabaseMetaData#typePredBasic
	 */
	BASIC;

	/**
	 * Based on the code retrieved from {@link DatabaseMetaData#getTypeInfo()} for the {@code SEARCHABLE}
	 * column, return the appropriate enum.
	 *
	 * @param code The retrieved code value.
	 *
	 * @return The corresponding enum.
	 */
	public static TypeSearchability interpret(short code) {
		return switch (code) {
			case DatabaseMetaData.typeSearchable -> FULL;
			case DatabaseMetaData.typePredNone -> NONE;
			case DatabaseMetaData.typePredBasic -> BASIC;
			case DatabaseMetaData.typePredChar -> CHAR;
			default -> throw new IllegalArgumentException( "Unknown type searchability code [" + code + "] encountered" );
		};
	}
}
