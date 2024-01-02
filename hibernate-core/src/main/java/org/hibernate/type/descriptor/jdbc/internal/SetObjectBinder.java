/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Support for binding values directly through `setObject` JDBC driver calls.
 *
 * @author Steve Ebersole
 */
public class SetObjectBinder<T> extends BasicBinder<T> {
	private final Class<?> baseClass;
	private final int jdbcTypeCode;

	public SetObjectBinder(
			JavaType<T> javaType,
			JdbcType jdbcType,
			Class<?> baseClass,
			int jdbcTypeCode) {
		super( javaType, jdbcType );
		this.baseClass = baseClass;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	protected void doBind(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
		st.setObject( index, normalize( value, options ), jdbcTypeCode );
	}

	protected Object normalize(T value, WrapperOptions options) {
		return getJavaType().unwrap( value, baseClass, options );
	}

	@Override
	protected void doBind(CallableStatement st, T value, String name, WrapperOptions options) throws SQLException {
		st.setObject( name, normalize( value, options ), jdbcTypeCode );
	}

	@Override
	protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
		st.setNull( index, jdbcTypeCode );
	}

	@Override
	protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
		st.setNull( name, jdbcTypeCode );
	}
}
