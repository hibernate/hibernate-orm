/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Emmanuel Bernard
 */
public class JodaMoneyCompositeUserType implements CompositeUserType<Money> {
	@Override
	public Object getPropertyValue(Money component, int property) throws HibernateException {
		return switch (property) {
			case 0 -> component.getCurrencyUnit().getCode();
			case 1 -> component.getAmount();
			default -> throw new HibernateException("unknown property");
		};
	}

	@Override
	public Money instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
		String currency = values.getValue(0, String.class);
		BigDecimal amount = values.getValue(1, BigDecimal.class);
		if (currency == null || amount == null) {
			return null;
		}
		return Money.of(CurrencyUnit.of(currency), amount);
	}

	@Override
	public Class<?> embeddable() {
		return Money.class;
	}

	@Override
	public Class<Money> returnedClass() {
		return Money.class;
	}

	@Override
	public boolean equals(Money x, Money y) {
		if (x == y) {
			return true;
		}
		if (x == null || y == null) {
			return false;
		}
		return x.equals(y);
	}

	@Override
	public int hashCode(Money x) {
		if (x == null) {
			return 0;
		}
		return x.hashCode();
	}

	@Override
	public Money deepCopy(Money value) {
		if (value == null) {
			return null;
		}
		return Money.of(value.getCurrencyUnit(), value.getAmount());
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Money value) {
		return value;
	}

	@Override
	public Money assemble(Serializable cached, Object owner) {
		return cached == null ? null : (Money) cached;
	}

	@Override
	public Money replace(Money detached, Money managed, Object owner) {
		return detached;
	}
}

