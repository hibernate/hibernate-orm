/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class EnumUserType<T extends Enum<T>> implements UserType<T>, ParameterizedType {

	private Class<T> clazz = null;

	public static <T extends Enum<T>> EnumUserType<T> createInstance(Class<T> clazz) {
		if ( !clazz.isEnum() ) {
			throw new IllegalArgumentException( "Parameter has to be an enum-class" );
		}
		EnumUserType<T> that = new EnumUserType<>();
		Properties p = new Properties();
		p.setProperty( "enumClassName", clazz.getName() );
		that.setParameterValues( p );
		return that;
	}

	public void setParameterValues(Properties params) {
		String enumClassName = params.getProperty( "enumClassName" );
		if ( enumClassName == null ) {
			throw new MappingException( "enumClassName parameter not specified" );
		}

		try {
			//noinspection unchecked
			this.clazz = (Class<T>) Class.forName( enumClassName );
		}
		catch (ClassNotFoundException e) {
			throw new MappingException( "enumClass " + enumClassName + " not found", e );
		}
		if ( !this.clazz.isEnum() ) {
			throw new MappingException( "enumClass " + enumClassName + " doesn't refer to an Enum" );
		}
	}

	@Override
	public int getSqlType() {
		return Types.CHAR;
	}

	public Class<T> returnedClass() {
		return clazz;
	}

	@Override
	public T nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		final String name = rs.getString( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return Enum.valueOf( clazz, name.trim() );
	}

	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index)
			throws HibernateException, SQLException {
	}

	@Override
	public void nullSafeSet(
			PreparedStatement preparedStatement,
			T value,
			int index,
			WrapperOptions options) throws SQLException {
		if ( null == value ) {
			preparedStatement.setNull( index, Types.VARCHAR );
		}
		else {
			preparedStatement.setString( index, value.name() );
		}
	}

	public T deepCopy(T value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public T assemble(Serializable cached, Object owner) throws HibernateException {
		//noinspection unchecked
		return (T) cached;
	}

	public Serializable disassemble(T value) throws HibernateException {
		return value;
	}

	public T replace(T original, T target, Object owner) throws HibernateException {
		return original;
	}

	public int hashCode(T x) throws HibernateException {
		return x.hashCode();
	}

	public boolean equals(T x, T y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( null == x || null == y ) {
			return false;
		}
		return x.equals( y );
	}
}
