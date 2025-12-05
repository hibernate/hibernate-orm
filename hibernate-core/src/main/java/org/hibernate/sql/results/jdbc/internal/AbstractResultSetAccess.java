/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import jakarta.persistence.EnumType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Base implementation of {@link ResultSetAccess}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractResultSetAccess implements ResultSetAccess {
	private final SharedSessionContractImplementor persistenceContext;
	private ResultSetMetaData resultSetMetaData;

	public AbstractResultSetAccess(SharedSessionContractImplementor persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	protected abstract SessionFactoryImplementor getFactory();

	protected SharedSessionContractImplementor getPersistenceContext() {
		return persistenceContext;
	}

	private SqlExceptionHelper getSqlExceptionHelper() {
		return getFactory().getJdbcServices().getSqlExceptionHelper();
	}

	private Dialect getDialect() {
		return getFactory().getJdbcServices().getDialect();
	}

	private ResultSetMetaData getResultSetMetaData() {
		if ( resultSetMetaData == null ) {
			try {
				resultSetMetaData = getResultSet().getMetaData();
			}
			catch (SQLException e) {
				throw getSqlExceptionHelper()
						.convert( e, "Unable to access ResultSetMetaData" );
			}
		}

		return resultSetMetaData;
	}

	@Override
	public int getColumnCount() {
		try {
			return getResultSetMetaData().getColumnCount();
		}
		catch (SQLException e) {
			throw getSqlExceptionHelper()
					.convert( e, "Unable to access ResultSet column count" );
		}
	}

	@Override
	public int resolveColumnPosition(String columnName) {
		try {
			return getResultSet().findColumn( normalizeColumnName( columnName ) );
		}
		catch (SQLException e) {
			throw getSqlExceptionHelper()
					.convert( e, "Unable to find column position by name: " + columnName );
		}
	}

	private String normalizeColumnName(String columnName) {
		return getFactory().getJdbcServices().getJdbcEnvironment().getIdentifierHelper()
				.toMetaDataObjectName( Identifier.toIdentifier( columnName ) );
	}

	@Override
	public String resolveColumnName(int position) {
		try {
			return getDialect().getColumnAliasExtractor()
					.extractColumnAlias( getResultSetMetaData(), position );
		}
		catch (SQLException e) {
			throw getSqlExceptionHelper()
					.convert( e, "Unable to find column name by position" );
		}
	}

	@Override
	public int getResultCountEstimate() {
		return -1;
	}

	@Override
	public <J> BasicType<J> resolveType(int position, JavaType<J> explicitJavaType, TypeConfiguration typeConfiguration) {
		try {
			final var metaData = getResultSetMetaData();
			final var registry = typeConfiguration.getJdbcTypeRegistry();
			final String columnTypeName = metaData.getColumnTypeName( position );
			final int columnType = metaData.getColumnType( position );
			final int scale = metaData.getScale( position );
			final int precision = metaData.getPrecision( position );
			final int displaySize = metaData.getColumnDisplaySize( position );
			final var dialect = getDialect();
			final int length = dialect.resolveSqlTypeLength( columnTypeName, columnType, precision, scale, displaySize );
			final var resolvedJdbcType =
					dialect.resolveSqlTypeDescriptor( columnTypeName, columnType, length, scale, registry );
			final var jdbcType =
					explicitJavaType == null
							? resolvedJdbcType
							: jdbcType( explicitJavaType, resolvedJdbcType, length, precision, scale, typeConfiguration );
			// If there is an explicit JavaType, then prefer its recommended JDBC type
			final JavaType<J> javaType =
					explicitJavaType == null
							? jdbcType.getJdbcRecommendedJavaTypeMapping( length, scale, typeConfiguration )
							: explicitJavaType;
			return typeConfiguration.getBasicTypeRegistry().resolve( javaType, jdbcType );
		}
		catch (SQLException e) {
			throw getSqlExceptionHelper()
					.convert( e, "Unable to determine JDBC type code for ResultSet position " + position );
		}
	}

	private <J> JdbcType jdbcType(
			JavaType<J> javaType,
			JdbcType resolvedJdbcType,
			int length, int precision, int scale,
			TypeConfiguration typeConfiguration) {
		return javaType.getRecommendedJdbcType(
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

					@Override
					public Dialect getDialect() {
						return AbstractResultSetAccess.this.getDialect();
					}
				}
		);
	}
}
