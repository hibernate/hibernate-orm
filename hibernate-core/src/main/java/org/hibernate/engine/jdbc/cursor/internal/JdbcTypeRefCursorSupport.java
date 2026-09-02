/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;

/// JDBC-type-based REF_CURSOR registration and untyped extraction.
///
/// @author Steve Ebersole
public final class JdbcTypeRefCursorSupport implements RefCursorSupport {
	private final int jdbcTypeCode;
	private final RefCursorSupportCreationContext creationContext;

	public JdbcTypeRefCursorSupport(
			int jdbcTypeCode,
			RefCursorSupportCreationContext creationContext) {
		this.jdbcTypeCode = jdbcTypeCode;
		this.creationContext = creationContext;
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
		try {
			statement.registerOutParameter( position, jdbcTypeCode );
		}
		catch (SQLException e) {
			throw creationContext.convert(
					e,
					"Error asking dialect to register ref cursor parameter [" + position + "]"
			);
		}
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
		try {
			statement.registerOutParameter( name, jdbcTypeCode );
		}
		catch (SQLException e) {
			throw creationContext.convert(
					e,
					"Error asking dialect to register ref cursor parameter [" + name + "]"
			);
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		try {
			return (ResultSet) statement.getObject( position );
		}
		catch (SQLException e) {
			throw creationContext.convert(
					e,
					"Error asking dialect to extract ResultSet from CallableStatement parameter [" + position + "]"
			);
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		try {
			return (ResultSet) statement.getObject( name );
		}
		catch (SQLException e) {
			throw creationContext.convert(
					e,
					"Error asking dialect to extract ResultSet from CallableStatement parameter [" + name + "]"
			);
		}
	}
}
