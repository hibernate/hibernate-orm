/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Descriptor for binding objects using any {@linkplain Types JDBC type code}.
 *
 * @author Steve Ebersole
 */
public class ObjectJdbcType implements JdbcType {
	/**
	 * An instance for the JDBC type code {@link Types#JAVA_OBJECT JAVA_OBJECT}.
	 */
	public static final ObjectJdbcType INSTANCE = new ObjectJdbcType( Types.JAVA_OBJECT );

	private final int jdbcTypeCode;

	/**
	 * Construct an instance for handling the given {@linkplain Types JDBC type code}.
	 *
	 * @param jdbcTypeCode any type code defined by {@link Types}.
	 */
	public ObjectJdbcType(int jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	@Override
	public String toString() {
		return "ObjectTypeDescriptor(" + jdbcTypeCode + ")";
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		if ( Serializable.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
			return VarbinaryJdbcType.INSTANCE.getBinder( javaType );
		}

		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, value, jdbcTypeCode );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, value, jdbcTypeCode );
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		if ( Serializable.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
			return VarbinaryJdbcType.INSTANCE.getExtractor( javaType );
		}

		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return (X) rs.getObject( paramIndex );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return (X) statement.getObject( index );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return (X) statement.getObject( name );
			}
		};
	}
}
