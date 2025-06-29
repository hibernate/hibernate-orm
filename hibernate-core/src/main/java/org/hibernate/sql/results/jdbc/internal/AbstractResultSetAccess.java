/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractResultSetAccess implements ResultSetAccess {
	private final SharedSessionContractImplementor persistenceContext;
	private final Dialect dialect;
	private ResultSetMetaData resultSetMetaData;

	public AbstractResultSetAccess(SharedSessionContractImplementor persistenceContext) {
		this.persistenceContext = persistenceContext;
		this.dialect = persistenceContext.getJdbcServices().getDialect();
	}

	protected SharedSessionContractImplementor getPersistenceContext() {
		return persistenceContext;
	}

	protected ResultSetMetaData getMetaData() {
		if ( resultSetMetaData == null ) {
			try {
				resultSetMetaData = getResultSet().getMetaData();
			}
			catch (SQLException e) {
				throw persistenceContext.getJdbcServices().getSqlExceptionHelper()
						.convert( e, "Unable to access ResultSetMetaData" );
			}
		}

		return resultSetMetaData;
	}

	@Override
	public int getColumnCount() {
		try {
			return getMetaData().getColumnCount();
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper()
					.convert( e, "Unable to access ResultSet column count" );
		}
	}

	@Override
	public int resolveColumnPosition(String columnName) {
		try {
			return getResultSet().findColumn(
					StringHelper.unquote( columnName, this.dialect )
			);
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper()
					.convert( e, "Unable to find column position by name: " + columnName );
		}
	}

	@Override
	public String resolveColumnName(int position) {
		try {
			return dialect
					.getColumnAliasExtractor()
					.extractColumnAlias( getMetaData(), position );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper()
					.convert( e, "Unable to find column name by position" );
		}
	}
}
