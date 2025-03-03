/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Descriptor for binding objects, but binding nulls with Types.VARBINARY
 *
 * @author Christian Beikov
 */
public class ObjectNullAsBinaryTypeJdbcType extends ObjectJdbcType {
	/**
	 * Singleton access
	 */
	public static final ObjectNullAsBinaryTypeJdbcType INSTANCE = new ObjectNullAsBinaryTypeJdbcType( Types.JAVA_OBJECT );

	public ObjectNullAsBinaryTypeJdbcType(int jdbcTypeCode) {
		super( jdbcTypeCode );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		if ( Serializable.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
			return VarbinaryJdbcType.INSTANCE.getBinder( javaType );
		}

		return new BasicBinder<>( javaType, this ) {

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options)
					throws SQLException {
				st.setNull( index, Types.VARBINARY );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options)
					throws SQLException {
				st.setNull( name, Types.VARBINARY );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, value, getJdbcTypeCode() );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, value, getJdbcTypeCode() );
			}
		};
	}
}
