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

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterBoolean;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BOOLEAN BOOLEAN} handling.
 *
 * @author Steve Ebersole
 */
public class BooleanJdbcType implements AdjustableJdbcType {
	public static final BooleanJdbcType INSTANCE = new BooleanJdbcType();

	public BooleanJdbcType() {
	}

	public int getJdbcTypeCode() {
		return Types.BOOLEAN;
	}

	@Override
	public String getFriendlyName() {
		return "BOOLEAN";
	}

	@Override
	public String toString() {
		return "BooleanTypeDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Boolean.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterBoolean<>( javaType );
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		final int preferredSqlTypeCodeForBoolean = indicators.getPreferredSqlTypeCodeForBoolean();
		// We treat BIT like BOOLEAN because it uses the same JDBC access methods
		if ( preferredSqlTypeCodeForBoolean != Types.BIT && preferredSqlTypeCodeForBoolean != Types.BOOLEAN ) {
			return indicators.getTypeConfiguration()
					.getJdbcTypeRegistry()
					.getDescriptor( preferredSqlTypeCodeForBoolean );
		}
		return this;
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Boolean.class;
	}

	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, options.getPreferredSqlTypeCodeForBoolean() );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, options.getPreferredSqlTypeCodeForBoolean() );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setBoolean( index, javaType.unwrap( value, Boolean.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setBoolean( name, javaType.unwrap( value, Boolean.class, options ) );
			}
		};
	}

	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getBoolean( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getBoolean( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getBoolean( name ), options );
			}
		};
	}
}
