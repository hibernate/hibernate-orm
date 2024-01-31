/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;

/**
 * @author Steve Ebersole
 */
public class FallbackRefCursorSupport implements RefCursorSupport {
	private final JdbcServices jdbcServices;

	public FallbackRefCursorSupport(JdbcServices jdbcServices) {
		this.jdbcServices = jdbcServices;
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
		try {
			jdbcServices.getDialect().registerResultSetOutParameter( statement, position );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert( e, "Error asking dialect to register ref cursor parameter [" + position + "]" );
		}
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
		try {
			jdbcServices.getDialect().registerResultSetOutParameter( statement, name );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert( e, "Error asking dialect to register ref cursor parameter [" + name + "]" );
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		try {
			return jdbcServices.getDialect().getResultSet( statement, position );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Error asking dialect to extract ResultSet from CallableStatement parameter [" + position + "]"
			);
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		try {
			return jdbcServices.getDialect().getResultSet( statement, name );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Error asking dialect to extract ResultSet from CallableStatement parameter [" + name + "]"
			);
		}
	}
}
