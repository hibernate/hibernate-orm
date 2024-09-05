/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.embeddables;

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
public class DollarValueUserType implements UserType<DollarValue> {

	@Override
	public int getSqlType() {
		return Types.BIGINT;
	}

	@Override
	public Class<DollarValue> returnedClass() {
		return DollarValue.class;
	}

	@Override
	public boolean equals(DollarValue x, DollarValue y) throws HibernateException {
		return x.getAmount().equals(y.getAmount());
	}

	@Override
	public int hashCode(DollarValue x) throws HibernateException {
		return x.getAmount().hashCode();
	}

	@Override
	public DollarValue nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session)
			throws SQLException {
		return new DollarValue( rs.getBigDecimal( position ) );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			DollarValue value,
			int index,
			SharedSessionContractImplementor session) throws SQLException {
		st.setBigDecimal(index, value.getAmount());
	}

	@Override
	public DollarValue deepCopy(DollarValue value) throws HibernateException {
		return new DollarValue();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(DollarValue value) throws HibernateException {
		return null;
	}

	@Override
	public DollarValue assemble(Serializable cached, Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	public DollarValue replace(DollarValue original, DollarValue target, Object owner)
			throws HibernateException {
		return null;
	}
}
