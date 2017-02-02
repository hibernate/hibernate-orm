/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cut;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Gavin King
 */
public class MonetoryAmountUserType implements CompositeUserType {

	@Override
	public String[] getPropertyNames() {
		return new String[] { "amount", "currency" };
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[] { StandardBasicTypes.BIG_DECIMAL, StandardBasicTypes.CURRENCY };
	}

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		MonetoryAmount ma = (MonetoryAmount) component;
		return property==0 ? ma.getAmount() : ma.getCurrency();
	}

	@Override
	public void setPropertyValue(Object component, int property, Object value)
			throws HibernateException {
		MonetoryAmount ma = (MonetoryAmount) component;
		if ( property==0 ) {
			ma.setAmount( (BigDecimal) value );
		}
		else {
			ma.setCurrency( (Currency) value );
		}
	}

	@Override
	public Class returnedClass() {
		return MonetoryAmount.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (x==y) return true;
		if (x==null || y==null) return false;
		MonetoryAmount mx = (MonetoryAmount) x;
		MonetoryAmount my = (MonetoryAmount) y;
		return mx.getAmount().equals( my.getAmount() ) &&
			mx.getCurrency().equals( my.getCurrency() );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return ( (MonetoryAmount) x ).getAmount().hashCode();
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		BigDecimal amt = StandardBasicTypes.BIG_DECIMAL.nullSafeGet( rs, names[0], session );
		Currency cur = StandardBasicTypes.CURRENCY.nullSafeGet( rs, names[1], session );
		if (amt==null) return null;
		return new MonetoryAmount(amt, cur);
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		MonetoryAmount ma = (MonetoryAmount) value;
		BigDecimal amt = ma == null ? null : ma.getAmount();
		Currency cur = ma == null ? null : ma.getCurrency();
		StandardBasicTypes.BIG_DECIMAL.nullSafeSet(st, amt, index, session);
		StandardBasicTypes.CURRENCY.nullSafeSet(st, cur, index+1, session);
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		MonetoryAmount ma = (MonetoryAmount) value;
		return new MonetoryAmount( ma.getAmount(), ma.getCurrency() );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session)
			throws HibernateException {
		return (Serializable) deepCopy(value);
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return deepCopy(cached);
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return deepCopy(original); //TODO: improve
	}

}
