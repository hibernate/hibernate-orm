/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import java.sql.Types;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.USE;

/// Describes how a database represents nationalized character data.
///
/// This contract describes database SQL-type semantics. It is intentionally
/// independent of whether a JDBC driver correctly implements nationalized
/// access methods, which is reported by
/// [Dialect#supportsNationalizedMethods()].
///
/// @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
/// @see org.hibernate.annotations.Nationalized
/// @see Dialect#getNationalizationSupport()
/// @see Dialect#supportsNationalizedMethods()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public enum NationalizationSupport {

	/// The ordinary `CHAR`, `VARCHAR`, and `CLOB` types inherently handle
	/// nationalized character data.
	IMPLICIT,

	/// The database defines distinct `NCHAR`, `NVARCHAR`, and `NCLOB` types.
	EXPLICIT,

	/// The database does not support nationalized character data.
	UNSUPPORTED;

	/// Return the JDBC type code used for a nationalized `CHAR` value.
	public int getCharVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.CHAR;
			case EXPLICIT -> Types.NCHAR;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}

	/// Return the JDBC type code used for a nationalized `VARCHAR` value.
	public int getVarcharVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.VARCHAR;
			case EXPLICIT -> Types.NVARCHAR;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}

	/// Return the JDBC type code used for a nationalized `LONGVARCHAR` value.
	public int getLongVarcharVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.LONGVARCHAR;
			case EXPLICIT -> Types.LONGNVARCHAR;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}

	/// Return the JDBC type code used for a nationalized `CLOB` value.
	public int getClobVariantCode() {
		return switch ( this ) {
			case IMPLICIT -> Types.CLOB;
			case EXPLICIT -> Types.NCLOB;
			case UNSUPPORTED -> throw new UnsupportedOperationException("Nationalized character data not supported on this database");
		};
	}
}
