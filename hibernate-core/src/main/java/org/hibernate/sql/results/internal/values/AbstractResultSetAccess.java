/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.values;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.spi.ResultSetAccess;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractResultSetAccess implements ResultSetAccess {
	private final SharedSessionContractImplementor persistenceContext;
	private ResultSetMetaData resultSetMetaData;

	public AbstractResultSetAccess(SharedSessionContractImplementor persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	protected SharedSessionContractImplementor getPersistenceContext() {
		return persistenceContext;
	}

	protected ResultSetMetaData getMetaData() {
		// todo (6.0) : we need to consider a way to abstract this from JDBC so we can re-use all of this code for cached results as well
		if ( resultSetMetaData == null ) {
			try {
				resultSetMetaData = getResultSet().getMetaData();
			}
			catch (SQLException e) {
				throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
						e,
						"Unable to access ResultSetMetaData"
				);
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
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper().convert(
					e,
					"Unable to access ResultSet column count"
			);
		}
	}

	@Override
	public int resolveColumnPosition(String columnName) {
		try {
			return getResultSet().findColumn( columnName );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper().convert(
					e,
					"Unable to find column position by name"
			);
		}
	}

	@Override
	public String resolveColumnName(int position) {
		try {
			return getFactory().getJdbcServices().
					getJdbcEnvironment()
					.getDialect()
					.getColumnAliasExtractor()
					.extractColumnAlias( getMetaData(), position );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper().convert(
					e,
					"Unable to find column name by position"
			);
		}
	}

	@Override
	public SqlTypeDescriptor resolveSqlTypeDescriptor(int position) {
		try {
			return getFactory().getTypeConfiguration()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( getMetaData().getColumnType( position ) );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to determine JDBC type code for ResultSet position " + position
			);
		}
	}
}
