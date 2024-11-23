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

public class ImmutableMonetoryAmountUserType implements CompositeUserType {

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
		ImmutableMonetoryAmount ma = (ImmutableMonetoryAmount) component;
		return property==0 ? ma.getAmount() : ma.getCurrency();
	}

	@Override
	public void setPropertyValue(Object component, int property, Object value)
			throws HibernateException {
		throw new UnsupportedOperationException("immutable");
	}

	@Override
	public Class returnedClass() {
		return ImmutableMonetoryAmount.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (x==y) return true;
		if (x==null || y==null) return false;
		ImmutableMonetoryAmount mx = (ImmutableMonetoryAmount) x;
		ImmutableMonetoryAmount my = (ImmutableMonetoryAmount) y;
		return mx.getAmount().equals( my.getAmount() ) &&
				mx.getCurrency().equals( my.getCurrency() );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return ( (ImmutableMonetoryAmount) x ).getAmount().hashCode();
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
		return new ImmutableMonetoryAmount(amt, cur);
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		ImmutableMonetoryAmount ma = (ImmutableMonetoryAmount) value;
		BigDecimal amt = ma == null ? null : ma.getAmount();
		Currency cur = ma == null ? null : ma.getCurrency();
		StandardBasicTypes.BIG_DECIMAL.nullSafeSet(st, amt, index, session);
		StandardBasicTypes.CURRENCY.nullSafeSet(st, cur, index+1, session);
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		ImmutableMonetoryAmount ma = (ImmutableMonetoryAmount) value;
		return new ImmutableMonetoryAmount( ma.getAmount(), ma.getCurrency() );
	}

	@Override
	public boolean isMutable() {
		return false;
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
