/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterCharacterData;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#VARCHAR VARCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class VarcharJdbcType implements AdjustableJdbcType {
	public static final VarcharJdbcType INSTANCE = new VarcharJdbcType();

	public VarcharJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public String getFriendlyName() {
		return "VARCHAR";
	}

	@Override
	public String toString() {
		return "VarcharTypeDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry()
				.resolveDescriptor( length != null && length == 1 ? Character.class : String.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterCharacterData<>( javaType );
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		assert domainJtd != null;
		return indicators.getTypeConfiguration().getJdbcTypeRegistry()
				.getDescriptor( indicators.resolveJdbcTypeCode( resolveIndicatedJdbcTypeCode( indicators ) ) );
	}

	protected int resolveIndicatedJdbcTypeCode(JdbcTypeIndicators indicators) {
		if ( indicators.isLob() ) {
			return indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else if ( shouldUseMaterializedLob( indicators ) ) {
			return indicators.isNationalized() ? SqlTypes.MATERIALIZED_NCLOB : SqlTypes.MATERIALIZED_CLOB;
		}
		else {
			return indicators.isNationalized() ? Types.NVARCHAR : Types.VARCHAR;
		}
	}

	protected boolean shouldUseMaterializedLob(JdbcTypeIndicators indicators) {
		final Dialect dialect = indicators.getDialect();
		final long length = indicators.getColumnLength();
		final long maxLength =
				indicators.isNationalized()
						? dialect.getMaxNVarcharCapacity()
						: dialect.getMaxVarcharCapacity();
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
				st.setString( index, javaType.unwrap( value, String.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, javaType.unwrap( value, String.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getString( name ), options );
			}
		};
	}
}
