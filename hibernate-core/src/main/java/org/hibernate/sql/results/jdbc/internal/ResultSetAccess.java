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
import java.sql.Types;

import javax.persistence.EnumType;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
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

	@Override
	default <J> BasicType<J> resolveType(int position, JavaTypeDescriptor<J> explicitJavaTypeDescriptor) {
		final TypeConfiguration typeConfiguration = getFactory().getTypeConfiguration();
		final JdbcServices jdbcServices = getFactory().getJdbcServices();
		try {
			final ResultSetMetaData metaData = getResultSet().getMetaData();
			final int columnType = metaData.getColumnType( position );
			final int scale = metaData.getScale( position );
			final int precision = metaData.getPrecision( position );
			final int length;
			if ( columnType == Types.CHAR && precision == 0 ) {
				length = metaData.getColumnDisplaySize( position );
			}
			else {
				length = precision;
			}
			final JdbcTypeDescriptor resolvedJdbcTypeDescriptor = jdbcServices.getDialect()
					.resolveSqlTypeDescriptor(
							columnType,
							length,
							scale,
							typeConfiguration.getJdbcTypeDescriptorRegistry()
					);
			final JavaTypeDescriptor<J> javaTypeDescriptor;
			final JdbcTypeDescriptor jdbcTypeDescriptor;
			// If there is an explicit JavaTypeDescriptor, then prefer its recommended JDBC type
			if ( explicitJavaTypeDescriptor != null ) {
				javaTypeDescriptor = explicitJavaTypeDescriptor;
				jdbcTypeDescriptor = explicitJavaTypeDescriptor.getRecommendedJdbcType(
						new JdbcTypeDescriptorIndicators() {
							@Override
							public TypeConfiguration getTypeConfiguration() {
								return typeConfiguration;
							}

							@Override
							public long getColumnLength() {
								return length;
							}

							@Override
							public EnumType getEnumeratedType() {
								return resolvedJdbcTypeDescriptor.isNumber() ? EnumType.ORDINAL : EnumType.STRING;
							}
						}
				);
			}
			else {
				jdbcTypeDescriptor = resolvedJdbcTypeDescriptor;
				javaTypeDescriptor = jdbcTypeDescriptor.getJdbcRecommendedJavaTypeMapping(
						length,
						scale,
						typeConfiguration
				);
			}
			return typeConfiguration.getBasicTypeRegistry().resolve( javaTypeDescriptor, jdbcTypeDescriptor );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to determine JDBC type code for ResultSet position " + position
			);
		}
	}

}
