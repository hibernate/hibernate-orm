/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * A custom SqlTypeDescriptor.  For example, this might be used to provide support
 * for a "non-standard" SQL type or to provide some special handling of values (e.g.
 * Oracle's dodgy handling of `""` as `null` but only in certain uses).
 *
 * This descriptor shows an example of replacing how VARCHAR values are handled.
 *
 * @author Steve Ebersole
 */
public class PayloadWrapperJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final PayloadWrapperJdbcType INSTANCE = new PayloadWrapperJdbcType();

	private PayloadWrapperJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		// given the Oracle example above we might want to replace the
		// handling of VARCHAR
		return Types.VARCHAR;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final String valueStr = javaType.unwrap( value, String.class, options );
				if ( StringHelper.isBlank( valueStr ) ) {
					st.setNull( index, getJdbcTypeCode() );
				}
				else {
					st.setString( index, valueStr );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
				final String valueStr = javaType.unwrap( value, String.class, options );
				if ( StringHelper.isBlank( valueStr ) ) {
					st.setNull( name, getJdbcTypeCode() );
				}
				else {
					st.setString( name, valueStr );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return VarcharJdbcType.INSTANCE.getExtractor( javaType );
	}
}
