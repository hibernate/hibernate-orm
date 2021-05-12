/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.sql.Types;

/**
 * Indicates how (if) the underlying database supports the use of nationalized data
 */
public enum NationalizationSupport {
	/**
	 * The database's CHAR, VARCHAR, LONGVARCHAR and CLOB types
	 * inherently handle nationalized data.  Generally speaking
	 * this means the database will not have dedicated nationalized
	 * data types (NCHAR, ...)
	 */
	IMPLICIT( Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB ),
	/**
	 * The database does define/support distinct nationalized
	 * data types (NCHAR, ...).
	 */
	EXPLICIT( Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.NCLOB ),
	/**
	 * The database does not define/support distinct nationalized
	 * data types (NCHAR, ...) and its corresponding base data
	 * types (CHAR, ...) do not support nationalized data
	 */
	UNSUPPORTED;

	private final int charVariantCode;
	private final int varcharVariantCode;
	private final int longVarcharVariantCode;
	private final int clobVariantCode;

	NationalizationSupport() { this( -1, -1, -1, -1 ); }

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
