/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Access to a JDBC ResultSet and information about it.
 *
 * @author Steve Ebersole
 */
public interface ResultSetAccess extends ResultSetMappingDescriptor.JdbcValuesMetadata {
	ResultSet getResultSet();
	SessionFactoryImplementor getFactory();
	void release();

	default int getColumnCount() {
		try {
			return getResultSet().getMetaData().getColumnCount();
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper().convert(
					e,
					"Unable to access ResultSet column count"
			);
		}
	}

	default int resolveColumnPosition(String columnName) {
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

	default String resolveColumnName(int position) {
		try {
			return getFactory().getJdbcServices().
					getJdbcEnvironment()
					.getDialect()
					.getColumnAliasExtractor()
					.extractColumnAlias( getResultSet().getMetaData(), position );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper().convert(
					e,
					"Unable to find column name by position"
			);
		}
	}

	default SqlTypeDescriptor resolveSqlTypeDescriptor(int position) {
		try {
			return getFactory().getTypeConfiguration()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( getResultSet().getMetaData().getColumnType( position ) );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to determine JDBC type code for ResultSet position " + position
			);
		}
	}

}
