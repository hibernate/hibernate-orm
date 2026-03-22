/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterCharacterData;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Descriptor for {@link Types#NVARCHAR NVARCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class NVarcharJdbcType implements AdjustableJdbcType {
	public static final NVarcharJdbcType INSTANCE = new NVarcharJdbcType();

	public NVarcharJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.NVARCHAR;
	}

	@Override
	public String getFriendlyName() {
		return "NVARCHAR";
	}

	@Override
	public String toString() {
		return "NVarcharTypeDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		if ( length != null && length == 1 ) {
			return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Character.class );
		}
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( String.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterCharacterData<>( javaType, true );
	}

	@Override
	public JdbcType resolveIndicatedType(
			JdbcTypeIndicators indicators,
			JavaType<?> domainJtd) {
		assert domainJtd != null;

		final var typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();

		final int jdbcTypeCode;
		if ( indicators.isLob() ) {
			jdbcTypeCode = indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else if ( shouldUseMaterializedLob( indicators ) ) {
			jdbcTypeCode = indicators.isNationalized() ? SqlTypes.MATERIALIZED_NCLOB : SqlTypes.MATERIALIZED_CLOB;
		}
		else {
			jdbcTypeCode = indicators.isNationalized() ? Types.NVARCHAR : Types.VARCHAR;
		}

		return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
	}

	protected boolean shouldUseMaterializedLob(JdbcTypeIndicators indicators) {
		final Dialect dialect = indicators.getDialect();
		final long length = indicators.getColumnLength();
		final long maxLength = indicators.isNationalized() ?
				dialect.getMaxNVarcharCapacity() :
				dialect.getMaxVarcharCapacity();
		return length > maxLength && dialect.useMaterializedLobWhenCapacityExceeded();
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return String.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					try {
						st.setNString( index, javaType.unwrap( value, String.class, options ) );
					}
					// workaround for jTDS driver for Sybase
					catch ( AbstractMethodError e ) {
						st.setBytes( index, javaType.unwrap( value, byte[].class, options ) );
					}
				}
				else {
					st.setString( index, javaType.unwrap( value, String.class, options ) );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					try {
						st.setNString( name, javaType.unwrap( value, String.class, options ) );
					}
					// workaround for jTDS driver for Sybase
					catch ( AbstractMethodError e ) {
						st.setBytes( name, javaType.unwrap( value, byte[].class, options ) );
					}
				}
				else {
					st.setString( name, javaType.unwrap( value, String.class, options ) );
				}
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					super.doBindNull( st, index, options );
				}
				else {
					st.setNull( index, Types.VARCHAR );
				}
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options)
					throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					super.doBindNull( st, name, options );
				}
				else {
					st.setNull( name, Types.VARCHAR );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					try {
						return javaType.wrap( rs.getNString( paramIndex ), options );
					}
					// workaround for jTDS driver for Sybase
					catch ( AbstractMethodError e ) {
						return javaType.wrap( rs.getBytes( paramIndex ), options );
					}
				}
				else {
					return javaType.wrap( rs.getString( paramIndex ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					try {
						return javaType.wrap( statement.getNString( index ), options );
					}
					// workaround for jTDS driver for Sybase
					catch ( AbstractMethodError e ) {
						return javaType.wrap( statement.getBytes( index ), options );
					}
				}
				else {
					return javaType.wrap( statement.getString( index ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				if ( options.getDialect().supportsNationalizedMethods() ) {
					try {
						return javaType.wrap( statement.getNString( name ), options );
					}
					// workaround for jTDS driver for Sybase
					catch ( AbstractMethodError e ) {
						return javaType.wrap( statement.getBytes( name ), options );
					}
				}
				else {
					return javaType.wrap( statement.getString( name ), options );
				}
			}
		};
	}
}
