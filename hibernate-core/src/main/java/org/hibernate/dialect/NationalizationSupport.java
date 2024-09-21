/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	IMPLICIT( Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB ),
	/**
	 * The database does define and support distinct SQL types
	 * for representing nationalized character data, typically
	 * named {@code NCHAR}, {@code NVARCHAR}, and {@code NCLOB}.
	 */
	EXPLICIT( Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.NCLOB ),
	/**
	 * The database does not even have support for nationalized
	 * character data.
	 */
	UNSUPPORTED;

	private final int charVariantCode;
	private final int varcharVariantCode;
	private final int longVarcharVariantCode;
	private final int clobVariantCode;

	NationalizationSupport() {
		this( -1, -1, -1, -1 );
	}

	NationalizationSupport(
			int charVariantCode,
			int varcharVariantCode,
			int longVarcharVariantCode,
			int clobVariantCode) {
		this.charVariantCode = charVariantCode;
		this.varcharVariantCode = varcharVariantCode;
		this.longVarcharVariantCode = longVarcharVariantCode;
		this.clobVariantCode = clobVariantCode;
	}

	public int getCharVariantCode() {
		return charVariantCode;
	}

	public int getVarcharVariantCode() {
		return varcharVariantCode;
	}

	public int getLongVarcharVariantCode() {
		return longVarcharVariantCode;
	}

	public int getClobVariantCode() {
		return clobVariantCode;
	}
}
