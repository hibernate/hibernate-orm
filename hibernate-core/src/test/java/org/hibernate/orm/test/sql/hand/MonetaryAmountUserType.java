/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * This is a simple Hibernate custom mapping type for MonetaryAmount value types.
 * <p>
 *
 * @author Max & Christian
 */
public class MonetaryAmountUserType implements CompositeUserType<MonetaryAmount> {

	@Override
	public Object getPropertyValue(MonetaryAmount component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return component.getCurrency();
			case 1:
				return component.getValue();
		}
		throw new HibernateException( "Illegal property index: " + property );
	}

	@Override
	public MonetaryAmount instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactory) {
		final BigDecimal value = valueAccess.getValue(0, BigDecimal.class);
		final Currency currency = valueAccess.getValue(1, Currency.class);

		if ( value == null && currency == null ) {
			return null;
		}
		return new MonetaryAmount( value, currency );
	}

	@Override
	public Class<?> embeddable() {
		return MonetaryAmountEmbeddable.class;
	}

	@Override
	public Class<MonetaryAmount> returnedClass() {
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

	public static class MonetaryAmountEmbeddable {
		private BigDecimal value;
		private Currency currency;
	}
}
