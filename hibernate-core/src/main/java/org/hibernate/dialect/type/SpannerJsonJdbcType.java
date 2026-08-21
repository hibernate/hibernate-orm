/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SpannerJsonJdbcType extends JsonJdbcType {
	private static final int VENDOR_TYPE_NUMBER = 100011;

	public static final SpannerJsonJdbcType INSTANCE = new SpannerJsonJdbcType( null );

	public SpannerJsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new SpannerJsonJdbcType( mappingType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws java.sql.SQLException {
				final String json = ( (SpannerJsonJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setObject( index, json, VENDOR_TYPE_NUMBER );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
				final String json = ( (SpannerJsonJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setObject( name, json, VENDOR_TYPE_NUMBER );
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, VENDOR_TYPE_NUMBER );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, VENDOR_TYPE_NUMBER );
			}
		};
	}
}
