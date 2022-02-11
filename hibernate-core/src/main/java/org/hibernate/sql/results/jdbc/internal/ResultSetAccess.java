/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import jakarta.persistence.EnumType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Access to a JDBC ResultSet and information about it.
 *
 * @author Steve Ebersole
 */
public interface ResultSetAccess extends JdbcValuesMetadata {
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

	default int resolveColumnPosition(String columnName, String tableName) {
		try {
			if ( tableName == null ) {
				return getResultSet().findColumn( columnName );
			}
			else {
				final ResultSetMetaData metaData = getResultSet().getMetaData();
				for ( int i = 1; i <= metaData.getColumnCount(); i++ ) {
					if ( columnName.equals( metaData.getColumnLabel( i ) )
							&& tableName.equals( metaData.getTableName( i ) ) ) {
						return i;
					}
				}
				return getResultSet().findColumn( columnName );
			}
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
			return getFactory().getJdbcServices().getJdbcEnvironment()
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

	default String resolveColumnTableName(int position) {
		try {
			return getResultSet().getMetaData().getTableName( position );
		}
		catch (SQLException e) {
			throw getFactory().getJdbcServices().getJdbcEnvironment().getSqlExceptionHelper().convert(
					e,
					"Unable to find column table name by position"
			);
		}
	}

	@Override
	default <J> BasicType<J> resolveType(
			int position,
			JavaType<J> explicitJavaType,
			SessionFactoryImplementor sessionFactory) {
		final TypeConfiguration typeConfiguration = getFactory().getTypeConfiguration();
		final JdbcServices jdbcServices = getFactory().getJdbcServices();
		try {
			final ResultSetMetaData metaData = getResultSet().getMetaData();
			final String columnTypeName = metaData.getColumnTypeName( position );
			final int columnType = metaData.getColumnType( position );
			final int scale = metaData.getScale( position );
			final int precision = metaData.getPrecision( position );
			final int displaySize = metaData.getColumnDisplaySize( position );
			final Dialect dialect = jdbcServices.getDialect();
			final int length = dialect.resolveSqlTypeLength(
					columnTypeName,
					columnType,
					precision,
					scale,
					displaySize
			);
			final JdbcType resolvedJdbcType = dialect
					.resolveSqlTypeDescriptor(
							columnTypeName,
							columnType,
							length,
							scale,
							typeConfiguration.getJdbcTypeRegistry()
					);
			final JavaType<J> javaType;
			final JdbcType jdbcType;
			// If there is an explicit JavaType, then prefer its recommended JDBC type
			if ( explicitJavaType != null ) {
				javaType = explicitJavaType;
				jdbcType = explicitJavaType.getRecommendedJdbcType(
						new JdbcTypeIndicators() {
							@Override
							public TypeConfiguration getTypeConfiguration() {
								return typeConfiguration;
							}

							@Override
							public long getColumnLength() {
								return length;
							}

							@Override
							public int getColumnPrecision() {
								return precision;
							}

							@Override
							public int getColumnScale() {
								return scale;
							}

							@Override
							public EnumType getEnumeratedType() {
								return resolvedJdbcType.isNumber() ? EnumType.ORDINAL : EnumType.STRING;
							}
						}
				);
			}
			else {
				jdbcType = resolvedJdbcType;
				javaType = jdbcType.getJdbcRecommendedJavaTypeMapping(
						length,
						scale,
						typeConfiguration
				);
			}
			return typeConfiguration.getBasicTypeRegistry().resolve( javaType, jdbcType );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to determine JDBC type code for ResultSet position " + position
			);
		}
	}

}
