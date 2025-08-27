/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.HibernateException;
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
		return switch ( property ) {
			case 0 -> component.getCurrency();
			case 1 -> component.getValue();
			default -> throw new HibernateException( "Illegal property index: " + property );
		};
	}

	@Override
	public MonetaryAmount instantiate(ValueAccess valueAccess) {
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
	public MonetaryAmount deepCopy(MonetaryAmount value) {
		return value; // MonetaryAmount is immutable
	}

	@Override
	public boolean equals(MonetaryAmount x, MonetaryAmount y) {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public Serializable disassemble(MonetaryAmount value) throws HibernateException {
		return value;
	}

	@Override
	public MonetaryAmount assemble(Serializable cached, Object owner) throws HibernateException {
		return (MonetaryAmount) cached;
	}

	@Override
	public MonetaryAmount replace(MonetaryAmount original, MonetaryAmount target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(MonetaryAmount x) throws HibernateException {
		return x.hashCode();
	}

	public static class MonetaryAmountEmbeddable {
		private BigDecimal value;
		private Currency currency;
	}
}
