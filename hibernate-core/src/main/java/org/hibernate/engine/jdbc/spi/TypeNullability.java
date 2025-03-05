/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.DatabaseMetaData;

/**
 * Describes the intrinsic nullability of a data type as reported by the JDBC driver.
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
		return switch (code) {
			case DatabaseMetaData.typeNullable -> NULLABLE;
			case DatabaseMetaData.typeNoNulls -> NON_NULLABLE;
			case DatabaseMetaData.typeNullableUnknown -> UNKNOWN;
			default -> throw new IllegalArgumentException( "Unknown type nullability code [" + code + "] encountered" );
		};
	}
}
