/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

/**
 * H2 requires binding JSON via {@code setBytes} methods.
 */
public class H2JsonJdbcType extends JsonJdbcType {
	/**
	 * Singleton access
	 */
	public static final H2JsonJdbcType INSTANCE = new H2JsonJdbcType( null );

	protected H2JsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.VARBINARY;
	}

	@Override
	public String toString() {
		return "H2JsonJdbcType";
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new H2JsonJdbcType( mappingType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String json = ( (H2JsonJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setBytes( index, json.getBytes( StandardCharsets.UTF_8 ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String json = ( (H2JsonJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
				st.setBytes( name, json.getBytes( StandardCharsets.UTF_8 ) );
			}
		};
	}
}
