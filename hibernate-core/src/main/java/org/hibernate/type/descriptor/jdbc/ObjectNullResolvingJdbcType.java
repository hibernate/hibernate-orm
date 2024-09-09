/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * Descriptor for binding objects, but binding nulls with the resolved parameter type
 *
 * @author Christian Beikov
 */
public class ObjectNullResolvingJdbcType extends ObjectJdbcType {
	/**
	 * Singleton access
	 */
	public static final ObjectNullResolvingJdbcType INSTANCE = new ObjectNullResolvingJdbcType( Types.JAVA_OBJECT );

	public ObjectNullResolvingJdbcType(int jdbcTypeCode) {
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
				if ( options.getDialect().supportsBindingNullForSetObject() ) {
					st.setObject( index, null );
				}
				else {
					final int sqlType = options.getDialect().supportsBindingNullSqlTypeForSetNull() ? Types.NULL
							: st.getParameterMetaData().getParameterType( index );
					st.setNull( index, sqlType );
				}
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options)
					throws SQLException {
				if ( options.getDialect().supportsBindingNullForSetObject() ) {
					st.setObject( name, null );
				}
				else {
					final int sqlType = options.getDialect().supportsBindingNullSqlTypeForSetNull() ? Types.NULL
							: Types.JAVA_OBJECT;
					st.setNull( name, sqlType );
				}
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
