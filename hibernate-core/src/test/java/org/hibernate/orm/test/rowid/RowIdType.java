/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.rowid;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * @author Gavin King
 */
public class RowIdType implements UserType<Object>{

	@Override
	public int getSqlType() {
		return Types.ROWID;
	}

	public Class<Object> returnedClass() {
		return Object.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		return x.equals(y);
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		return rs.getObject( position );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return null;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return null;
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return null;
	}

}
