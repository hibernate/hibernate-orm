/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * @author Yanming Zhou
 */
public class MyGenericType implements UserType<Object>, DynamicParameterizedType {

	private ParameterType parameterType;

	public ParameterType getParameterType() {
		return parameterType;
	}

	@Override
	public void setParameterValues(Properties params) {
		parameterType = (ParameterType) params.get(PARAMETER_TYPE);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options)
			throws SQLException {
		st.setString( index, null );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		return null;
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Class<Object> returnedClass() {
		return Object.class;
	}

	@Override
	public boolean equals(Object x, Object y) {
		return ( x == null && y == null ) || ( x != null && x.equals( y ) );
	}

	@Override
	public int hashCode(Object x) {
		return x.hashCode();
	}

	@Override
	public Object deepCopy(Object value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) {
		return null;
	}

	@Override
	public String assemble(Serializable cached, Object owner) {
		return (String) cached;
	}

}
