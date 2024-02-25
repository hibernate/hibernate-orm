/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;

/**
 * @author Jan Schatteman
 */
public class PostgreSQLUUIDJdbcType extends UUIDJdbcType {

	/**
	 * Singleton access
	 */
	public static final PostgreSQLUUIDJdbcType INSTANCE = new PostgreSQLUUIDJdbcType();

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, getJdbcType().getJdbcTypeCode(), "uuid" );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, getJdbcType().getJdbcTypeCode(), "uuid" );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, getJavaType().unwrap( value, UUID.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, getJavaType().unwrap( value, UUID.class, options ) );
			}
		};
	}
}
