/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

/**
 * This is a simple Hibernate custom mapping type for MonetaryAmount value types.
 * <p>
 * 
 * @author Max & Christian 
 */
public class MonetaryAmountUserType
		implements UserType {

	private static final int[] SQL_TYPES = {Types.NUMERIC, Types.VARCHAR };

	public int[] sqlTypes() { return SQL_TYPES; }

	public Class returnedClass() { return MonetaryAmount.class; }

	public boolean isMutable() { return false; }

	public Object deepCopy(Object value) {
		return value; // MonetaryAmount is immutable
	}

	public boolean equals(Object x, Object y) {
		if (x == y) return true;
		if (x == null || y == null) return false;
		return x.equals(y);
	}

	public Object nullSafeGet(ResultSet resultSet,
							  String[] names,
							  SessionImplementor session, Object owner)
			throws HibernateException, SQLException {

		BigDecimal value = resultSet.getBigDecimal(names[0]);
		if (resultSet.wasNull()) return null;
		String cur = resultSet.getString(names[1]);
		Currency userCurrency = Currency.getInstance(cur);
						
		return new MonetaryAmount(value, userCurrency);
	}

	public void nullSafeSet(PreparedStatement statement,
							Object value,
							int index, SessionImplementor session)
			throws HibernateException, SQLException {

		if (value == null) {
			statement.setNull(index, Types.NUMERIC);			
			statement.setNull(index+1, Types.VARCHAR);
		} else {
			MonetaryAmount currency = (MonetaryAmount)value;
			statement.setBigDecimal(index, currency.getValue());
			statement.setString(index+1, currency.getCurrency().getCurrencyCode());
		}
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	public Object replace(Object original, Object target, Object owner)
	throws HibernateException {
		return original;
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}
}
