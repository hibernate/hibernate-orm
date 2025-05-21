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
 * @author Steve Ebersole
 */
public class Helper {
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

	public static void setLockTimeout(
			Timeout timeout,
			Function<Timeout,Integer> valueStrategy,
			String sqlFormat,
			Connection connection,
			SessionFactoryImplementor factory) {
		final int milliseconds = valueStrategy.apply( timeout );

		final String sql = String.format( sqlFormat, milliseconds );
		try (final java.sql.Statement statement = connection.createStatement()) {
			factory.getJdbcServices().getSqlStatementLogger().logStatement( sql );
			statement.execute( sql );
		}
		catch (SQLException sqle) {
			final SqlExceptionHelper sqlExceptionHelper = factory.getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper();
			throw sqlExceptionHelper.convert( sqle, "Unable to set lock-timeout setting on JDBC connection" );
		}
	}

	@FunctionalInterface
	public interface TimeoutExtractor {
		Timeout extractFrom(ResultSet resultSet) throws SQLException;
	}
}
