/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.DatabaseMetaData;

/**
 * Describes the instrinsic nullability of a data type as reported by the JDBC driver.
 *
 * @author Steve Ebersole
 */
public enum TypeNullability {
	/**
	 * The data type can accept nulls
	 * @see DatabaseMetaData#typeNullable
	 */
	NULLABLE,
	/**
	 * The data type cannot accept nulls
	 * @see DatabaseMetaData#typeNoNulls
	 */
	NON_NULLABLE,
	/**
	 * It is unknown if the data type accepts nulls
	 * @see DatabaseMetaData#typeNullableUnknown
	 */
	UNKNOWN;

	/**
	 * Based on the code retrieved from {@link DatabaseMetaData#getTypeInfo()} for the {@code NULLABLE}
	 * column, return the appropriate enum.
	 *
	 * @param code The retrieved code value.
	 *
	 * @return The corresponding enum.
	 */
	public static TypeNullability interpret(short code) {
		switch ( code ) {
			case DatabaseMetaData.typeNullable: {
				return NULLABLE;
			}
			case DatabaseMetaData.typeNoNulls: {
				return NON_NULLABLE;
			}
			case DatabaseMetaData.typeNullableUnknown: {
				return UNKNOWN;
			}
			default: {
				throw new IllegalArgumentException( "Unknown type nullability code [" + code + "] enountered" );
			}
		}
	}
}
