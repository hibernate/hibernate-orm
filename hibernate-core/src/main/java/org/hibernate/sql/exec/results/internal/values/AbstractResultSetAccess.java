/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal.values;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.tree.spi.select.ResultSetAccess;
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
			throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error calling ResultSetMetaData#getColumnCount"
			);
		}
	}

	@Override
	public int resolveColumnPosition(String columnName) {
		try {
			return getResultSet().findColumn( columnName );
		}
		catch (SQLException e) {
			throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to resolve column position for `" + columnName + "` using ResultSet#findColumn"
			);
		}
	}

	@Override
	public String resolveColumnName(int position) {
		try {
			return persistenceContext.getJdbcServices()
					.getJdbcEnvironment()
					.getDialect()
					.getColumnAliasExtractor()
					.extractColumnAlias( getMetaData(), position );
		}
		catch (SQLException e) {
			throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to resolve column name/label for ResultSet position " + position
			);
		}
	}

	@Override
	public SqlTypeDescriptor resolveSqlTypeDescriptor(int position) {
		try {
			return persistenceContext.getFactory()
					.getTypeConfiguration()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( getMetaData().getColumnType( position ) );
		}
		catch (SQLException e) {
			throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to determine JDBC type code for ResultSet position " + position
			);
		}
	}
}
