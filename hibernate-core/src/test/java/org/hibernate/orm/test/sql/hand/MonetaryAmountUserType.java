/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * This is a simple Hibernate custom mapping type for MonetaryAmount value types.
 * <p>
 *
 * @author Max & Christian
 */
public class MonetaryAmountUserType implements UserType {

	private static final int[] SQL_TYPES = {Types.NUMERIC, Types.VARCHAR};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class returnedClass() {
		return MonetaryAmount.class;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object deepCopy(Object value) {
		return value; // MonetaryAmount is immutable
	}

	@Override
	public boolean equals(Object x, Object y) {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		// needs CompositeUserType
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement statement,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			statement.setNull( index, Types.NUMERIC );
			statement.setNull( index + 1, Types.VARCHAR );
		}
		else {
			MonetaryAmount currency = (MonetaryAmount) value;
			statement.setBigDecimal( index, currency.getValue() );
			statement.setString( index + 1, currency.getCurrency().getCurrencyCode() );
		}
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}
}
