/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Helper for dealing with {@linkplain Connection}-level lock timeouts.
 *
 * @author Steve Ebersole
 */
public class Helper {
	/**
	 * Use the given {@code sql} statement to query the current lock-timeout for the
	 * {@linkplain Connection} and use the {@code extractor} to process the value.
	 */
	public static Timeout getLockTimeout(
			String sql,
			TimeoutExtractor extractor,
			Connection connection,
			SessionFactoryImplementor factory) {
		try (final java.sql.Statement statement = connection.createStatement()) {
			factory.getJdbcServices().getSqlStatementLogger().logStatement( sql );
			final ResultSet results = statement.executeQuery( sql );
			if ( !results.next() ) {
				throw new HibernateException( "Unable to query JDBC Connection for current lock-timeout setting (no result)" );
			}
			return extractor.extractFrom( results );
		}
		catch (SQLException sqle) {
			final SqlExceptionHelper sqlExceptionHelper = factory.getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper();
			throw sqlExceptionHelper.convert( sqle, "Unable to query JDBC Connection for current lock-timeout setting" );
		}
	}

	/**
	 * Set the {@linkplain Connection}-level lock-timeout using the given {@code sql} command.
	 */
	public static void setLockTimeout(
			String sql,
			Connection connection,
			SessionFactoryImplementor factory) {
		try (final java.sql.Statement statement = connection.createStatement()) {
			factory.getJdbcServices().getSqlStatementLogger().logStatement( sql );
			statement.execute( sql );
		}
		catch (SQLException sqle) {
			final SqlExceptionHelper sqlExceptionHelper = factory.getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper();
			throw sqlExceptionHelper.convert( sqle, "Unable to set lock-timeout setting on JDBC connection" );
		}
	}

	/**
	 * Set the {@linkplain Connection}-level lock-timeout using
	 * the given {@code sqlFormat} (with a single format placeholder
	 * for the {@code milliseconds} value).
	 *
	 * @see #setLockTimeout(String, Connection, SessionFactoryImplementor)
	 */
	public static void setLockTimeout(
			Integer milliseconds,
			String sqlFormat,
			Connection connection,
			SessionFactoryImplementor factory) {
		final String sql = String.format( sqlFormat, milliseconds );
		setLockTimeout( sql, connection, factory );
	}

	/**
	 * Set the {@linkplain Connection}-level lock-timeout.  The passed
	 * {@code valueStrategy} is used to interpret the {@code timeout}
	 * which is then used with {@code sqlFormat} to execute the command.
	 *
	 * @see #setLockTimeout(Integer, String, Connection, SessionFactoryImplementor)
	 */
	public static void setLockTimeout(
			Timeout timeout,
			Function<Timeout,Integer> valueStrategy,
			String sqlFormat,
			Connection connection,
			SessionFactoryImplementor factory) {
		final int milliseconds = valueStrategy.apply( timeout );
		setLockTimeout( milliseconds, sqlFormat, connection, factory );
	}

	@FunctionalInterface
	public interface TimeoutExtractor {
		Timeout extractFrom(ResultSet resultSet) throws SQLException;
	}
}
