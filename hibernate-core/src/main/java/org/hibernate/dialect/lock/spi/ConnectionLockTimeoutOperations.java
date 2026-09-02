/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.ToIntFunction;

import jakarta.persistence.Timeout;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.lock.internal.Helper;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Executes the common JDBC operations used by connection-level lock-timeout
/// strategies.
///
/// These operations integrate with Hibernate's SQL statement logger, statement
/// observer, resource handling, and SQL exception conversion. A provider-owned
/// [ConnectionLockTimeoutStrategy] should use them instead of depending on the
/// corresponding internal helper.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI(USE)
public final class ConnectionLockTimeoutOperations {
	private ConnectionLockTimeoutOperations() {
	}

	/// Queries the current timeout and extracts it from the first result row.
	public static Timeout query(
			String sql,
			TimeoutExtractor extractor,
			Connection connection,
			SessionFactoryImplementor factory) {
		Objects.requireNonNull( sql, "sql" );
		Objects.requireNonNull( extractor, "extractor" );
		Objects.requireNonNull( connection, "connection" );
		Objects.requireNonNull( factory, "factory" );
		return Helper.getLockTimeout( sql, extractor::extract, connection, factory );
	}

	/// Executes a complete connection-level lock-timeout command.
	public static void execute(
			String sql,
			Connection connection,
			SessionFactoryImplementor factory) {
		Helper.setLockTimeout(
				Objects.requireNonNull( sql, "sql" ),
				Objects.requireNonNull( connection, "connection" ),
				Objects.requireNonNull( factory, "factory" )
		);
	}

	/// Formats one integral value into a command and executes it.
	public static void execute(
			int value,
			String sqlFormat,
			Connection connection,
			SessionFactoryImplementor factory) {
		Objects.requireNonNull( sqlFormat, "sqlFormat" );
		execute( String.format( sqlFormat, value ), connection, factory );
	}

	/// Converts a timeout to one integral database value, formats it into a
	/// command, and executes the command.
	public static void execute(
			Timeout timeout,
			ToIntFunction<Timeout> valueResolver,
			String sqlFormat,
			Connection connection,
			SessionFactoryImplementor factory) {
		Objects.requireNonNull( timeout, "timeout" );
		Objects.requireNonNull( valueResolver, "valueResolver" );
		Objects.requireNonNull( sqlFormat, "sqlFormat" );
		Objects.requireNonNull( connection, "connection" );
		Objects.requireNonNull( factory, "factory" );
		execute( valueResolver.applyAsInt( timeout ), sqlFormat, connection, factory );
	}

	/// Extracts a timeout from the current row of a JDBC result set.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	@FunctionalInterface
	@SPI({ USE, IMPLEMENT })
	public interface TimeoutExtractor {
		Timeout extract(ResultSet resultSet) throws SQLException;
	}
}
