/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.embeddables;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Chris Pheby
 */
public class MyDateUserType implements UserType {

	@Override
	public int[] sqlTypes() {
		return new int[] {Types.DATE};
	}

	@Override
	public Class<MyDate> returnedClass() {
		return MyDate.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (!(x instanceof MyDate) || !(y instanceof MyDate)) {
			throw new HibernateException("Expected MyDate");
		}
		return ((MyDate)x).getDate().equals(((MyDate)y).getDate());
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		if (!(x instanceof MyDate)) {
			throw new HibernateException("Expected MyDate");
		}
		return ((MyDate)x).getDate().hashCode();
	}

	@Override
	public MyDate nullSafeGet(ResultSet rs, String[] names,
			SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		
		MyDate result = new MyDate(rs.getDate(rs.findColumn(names[0])));
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws HibernateException, SQLException {
		
		st.setDate(index, new java.sql.Date(((MyDate)value).getDate().getTime()));
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		MyDate result = new MyDate();

		return result;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return null;
	}

	@Override
	public Object assemble(Serializable cached, Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	public Object replace(Object original, Object target, Object owner)
			throws HibernateException {
		return null;
	}
}
