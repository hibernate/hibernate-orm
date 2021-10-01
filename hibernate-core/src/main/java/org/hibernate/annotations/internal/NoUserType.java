/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class NoUserType implements UserType<Void> {
	@Override
	public int[] sqlTypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<Void> returnedClass() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Void nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Void value, int index, SharedSessionContractImplementor session) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isMutable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		throw new UnsupportedOperationException();
	}
}
