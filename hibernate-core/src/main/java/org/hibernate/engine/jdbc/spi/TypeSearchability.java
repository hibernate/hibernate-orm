/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
