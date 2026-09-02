/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.temptable.internal.DB2GlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.internal.HSQLLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.internal.MySQLLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.internal.OracleLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.internal.SQLServerLocalTemporaryTableStrategy;

import static org.hibernate.SPI.Role.USE;

/// Provides stable stock temporary-table strategies without exposing
/// Hibernate's vendor-specific implementations.
///
/// Use one of these complete profiles only when its naming, DDL, column, and
/// before/after-use behavior matches the database. Providers with different
/// behavior should implement [TemporaryTableStrategy] or extend one of the
/// supported standard family strategies.
///
/// @see org.hibernate.dialect.Dialect#getGlobalTemporaryTableStrategy()
/// @see org.hibernate.dialect.Dialect#getLocalTemporaryTableStrategy()
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class TemporaryTableStrategies {
	private TemporaryTableStrategies() {
	}

	/// Returns the stable DB2-family global temporary-table strategy.
	///
	/// @since 8.0
	@SPI(USE)
	public static TemporaryTableStrategy db2Global() {
		return DB2GlobalTemporaryTableStrategy.INSTANCE;
	}

	/// Returns the stable HSQL-family local temporary-table strategy.
	///
	/// @since 8.0
	@SPI(USE)
	public static TemporaryTableStrategy hsqlLocal() {
		return HSQLLocalTemporaryTableStrategy.INSTANCE;
	}

	/// Returns the stable MySQL-family local temporary-table strategy.
	///
	/// @since 8.0
	@SPI(USE)
	public static TemporaryTableStrategy mysqlLocal() {
		return MySQLLocalTemporaryTableStrategy.INSTANCE;
	}

	/// Returns the stable Oracle-family local temporary-table strategy.
	///
	/// @since 8.0
	@SPI(USE)
	public static TemporaryTableStrategy oracleLocal() {
		return OracleLocalTemporaryTableStrategy.INSTANCE;
	}

	/// Returns the stable SQL Server-family local temporary-table strategy.
	///
	/// @since 8.0
	@SPI(USE)
	public static TemporaryTableStrategy sqlServerLocal() {
		return SQLServerLocalTemporaryTableStrategy.INSTANCE;
	}
}
