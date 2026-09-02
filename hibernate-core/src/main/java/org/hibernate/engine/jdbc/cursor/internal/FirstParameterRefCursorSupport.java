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

/// PostgreSQL-family REF_CURSOR fallback based on the first positional
/// parameter.
///
/// @author Steve Ebersole
public final class FirstParameterRefCursorSupport implements RefCursorSupport {
	private final String databaseName;
	private final int jdbcTypeCode;
	private final RefCursorSupportCreationContext creationContext;

	public FirstParameterRefCursorSupport(
			String databaseName,
			int jdbcTypeCode,
			RefCursorSupportCreationContext creationContext) {
		this.databaseName = databaseName;
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
		throw new UnsupportedOperationException(
				databaseName + " only supports registering REF_CURSOR parameters by position"
		);
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		if ( position != 1 ) {
			throw new UnsupportedOperationException(
					databaseName + " only supports REF_CURSOR parameters as the first parameter"
			);
		}
		try {
			return (ResultSet) statement.getObject( 1 );
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
		throw new UnsupportedOperationException(
				databaseName + " only supports accessing REF_CURSOR parameters by position"
		);
	}
}
