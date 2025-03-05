/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * @author Chris Pheby
 */
public class MyDateUserType implements UserType<MyDate> {

	@Override
	public int getSqlType() {
		return Types.DATE;
	}

	@Override
	public Class<MyDate> returnedClass() {
		return MyDate.class;
	}

	@Override
	public boolean equals(MyDate x, MyDate y) throws HibernateException {
		return x.getDate().equals(y.getDate());
	}

	@Override
	public int hashCode(MyDate x) throws HibernateException {
		return x.getDate().hashCode();
	}

	@Override
	public MyDate nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		return new MyDate( rs.getDate( position ) );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			MyDate value,
			int index,
			WrapperOptions options) throws SQLException {
		st.setDate(index, new java.sql.Date(value.getDate().getTime()));
	}

	@Override
	public MyDate deepCopy(MyDate value) throws HibernateException {
		MyDate result = new MyDate();

		return result;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(MyDate value) throws HibernateException {
		return null;
	}

	@Override
	public MyDate assemble(Serializable cached, Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	public MyDate replace(MyDate original, MyDate target, Object owner)
			throws HibernateException {
		return null;
	}
}
