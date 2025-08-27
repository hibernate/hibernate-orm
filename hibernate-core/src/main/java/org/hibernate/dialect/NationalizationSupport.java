/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.Types;

/**
 * Indicates if and how a database supports the use of nationalized
 * character data (Unicode).
 *
 * @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
 * @see org.hibernate.annotations.Nationalized
 * @see Dialect#getNationalizationSupport()
 */
public enum NationalizationSupport {

	/**
	 * The {@code CHAR}, {@code VARCHAR}, and {@code CLOB}
	 * types inherently handle nationalized character data.
	 * Usually the database will not even define dedicated
	 * nationalized data types like {@code NVARCHAR}.
	 */
	IMPLICIT,

	/**
	 * The database does define and support distinct SQL types
	 * for representing nationalized character data, typically
	 * named {@code NCHAR}, {@code NVARCHAR}, and {@code NCLOB}.
	 */
	EXPLICIT,

	/**
	 * The database does not even have support for nationalized
	 * character data.
	 */
	UNSUPPORTED;

	public int getCharVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.CHAR;
			case EXPLICIT -> Types.NCHAR;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}

	public int getVarcharVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.VARCHAR;
			case EXPLICIT -> Types.NVARCHAR;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}

	public int getLongVarcharVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.LONGVARCHAR;
			case EXPLICIT -> Types.LONGNVARCHAR;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}

	public int getClobVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.CLOB;
			case EXPLICIT -> Types.NCLOB;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}
}
