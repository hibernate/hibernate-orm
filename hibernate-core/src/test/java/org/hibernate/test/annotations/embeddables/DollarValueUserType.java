/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Chris Pheby
 */
public class DollarValueUserType implements UserType {

	@Override
	public int[] sqlTypes() {
		return new int[] {Types.BIGINT};
	}

	@Override
	public Class<DollarValue> returnedClass() {
		return DollarValue.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (!(x instanceof DollarValue) || !(y instanceof DollarValue)) {
			throw new HibernateException("Expected DollarValue");
		}
		return ((DollarValue)x).getAmount().equals(((DollarValue)y).getAmount());
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		if (!(x instanceof DollarValue)) {
			throw new HibernateException("Expected DollarValue");
		}
		return ((DollarValue)x).getAmount().hashCode();
	}

	@Override
	public DollarValue nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		return new DollarValue( rs.getBigDecimal( rs.findColumn( names[0])));
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		st.setBigDecimal(index, ((DollarValue)value).getAmount());
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return new DollarValue();
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
