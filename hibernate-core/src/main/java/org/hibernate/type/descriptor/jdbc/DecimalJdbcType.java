/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterNumericData;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#DECIMAL DECIMAL} handling.
 *
 * @author Steve Ebersole
 */
public class DecimalJdbcType implements JdbcType {
	public static final DecimalJdbcType INSTANCE = new DecimalJdbcType();

	public DecimalJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.DECIMAL;
	}

	@Override
	public String getFriendlyName() {
		return "DECIMAL";
	}

	@Override
	public String toString() {
		return "DecimalTypeDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( BigDecimal.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterNumericData<>( javaType, BigDecimal.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return BigDecimal.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setBigDecimal( index, javaType.unwrap( value, BigDecimal.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setBigDecimal( name, javaType.unwrap( value, BigDecimal.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getBigDecimal( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getBigDecimal( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getBigDecimal( name ), options );
			}
		};
	}
}
