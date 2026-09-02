/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.DB2StructJdbcType;
import org.hibernate.dialect.type.internal.DB2TemporalJdbcTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock DB2 JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and contribute the returned descriptor through the supplied type
/// contributions. Do not depend on the descriptor's concrete implementation
/// class.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class DB2JdbcTypes {
	private DB2JdbcTypes() {
	}

	/// Obtain the standard DB2 structured JDBC type.
	public static JdbcType struct() {
		return DB2StructJdbcType.INSTANCE;
	}

	/// Obtain DB2's standard instant descriptor with null-safe JDBC extraction.
	public static JdbcType instant() {
		return DB2TemporalJdbcTypes.instant();
	}

	/// Obtain DB2's standard local-date descriptor with null-safe JDBC extraction.
	public static JdbcType localDate() {
		return DB2TemporalJdbcTypes.localDate();
	}

	/// Obtain DB2's standard local-time descriptor with null-safe JDBC extraction.
	public static JdbcType localTime() {
		return DB2TemporalJdbcTypes.localTime();
	}

	/// Obtain DB2's standard local-date-time descriptor with null-safe JDBC extraction.
	public static JdbcType localDateTime() {
		return DB2TemporalJdbcTypes.localDateTime();
	}

	/// Obtain DB2's standard offset-time descriptor with null-safe JDBC extraction.
	public static JdbcType offsetTime() {
		return DB2TemporalJdbcTypes.offsetTime();
	}

	/// Obtain DB2's standard offset-date-time descriptor with null-safe JDBC extraction.
	public static JdbcType offsetDateTime() {
		return DB2TemporalJdbcTypes.offsetDateTime();
	}

	/// Obtain DB2's standard zoned-date-time descriptor with null-safe JDBC extraction.
	public static JdbcType zonedDateTime() {
		return DB2TemporalJdbcTypes.zonedDateTime();
	}
}
