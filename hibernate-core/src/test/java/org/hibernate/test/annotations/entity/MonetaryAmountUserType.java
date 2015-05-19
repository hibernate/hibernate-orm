/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Emmanuel Bernard
 */
public class MonetaryAmountUserType implements CompositeUserType {

	public String[] getPropertyNames() {
		return new String[]{"amount", "currency"};
	}

	public Type[] getPropertyTypes() {
		return new Type[]{ StandardBasicTypes.BIG_DECIMAL, StandardBasicTypes.CURRENCY };
	}

	public Object getPropertyValue(Object component, int property) throws HibernateException {
		MonetaryAmount ma = (MonetaryAmount) component;
		return property == 0 ? ma.getAmount() : ma.getCurrency();
	}

	public void setPropertyValue(Object component, int property, Object value)
			throws HibernateException {
		MonetaryAmount ma = (MonetaryAmount) component;
		if ( property == 0 ) {
			ma.setAmount( (BigDecimal) value );
		}
		else {
			ma.setCurrency( (Currency) value );
		}
	}

	public Class returnedClass() {
		return MonetaryAmount.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;
		MonetaryAmount mx = (MonetaryAmount) x;
		MonetaryAmount my = (MonetaryAmount) y;
		return mx.getAmount().equals( my.getAmount() ) &&
				mx.getCurrency().equals( my.getCurrency() );
	}

	public int hashCode(Object x) throws HibernateException {
		return ( (MonetaryAmount) x ).getAmount().hashCode();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		BigDecimal amt = StandardBasicTypes.BIG_DECIMAL.nullSafeGet( rs, names[0], session);
		Currency cur = StandardBasicTypes.CURRENCY.nullSafeGet( rs, names[1], session );
		if ( amt == null ) return null;
		return new MonetaryAmount( amt, cur );
	}

	public void nullSafeSet(
			PreparedStatement st, Object value, int index,
			SessionImplementor session
	) throws HibernateException, SQLException {
		MonetaryAmount ma = (MonetaryAmount) value;
		BigDecimal amt = ma == null ? null : ma.getAmount();
		Currency cur = ma == null ? null : ma.getCurrency();
		StandardBasicTypes.BIG_DECIMAL.nullSafeSet( st, amt, index, session );
		StandardBasicTypes.CURRENCY.nullSafeSet( st, cur, index + 1, session );
	}

	public Object deepCopy(Object value) throws HibernateException {
		MonetaryAmount ma = (MonetaryAmount) value;
		return new MonetaryAmount( ma.getAmount(), ma.getCurrency() );
	}

	public boolean isMutable() {
		return true;
	}

	public Serializable disassemble(Object value, SessionImplementor session)
			throws HibernateException {
		return (Serializable) deepCopy( value );
	}

	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
			throws HibernateException {
		return deepCopy( cached );
	}

	public Object replace(Object original, Object target, SessionImplementor session, Object owner)
			throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}

}
