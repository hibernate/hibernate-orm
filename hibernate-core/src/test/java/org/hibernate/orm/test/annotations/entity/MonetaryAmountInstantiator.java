/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * @author Steve Ebersole
 */
public class MonetaryAmountInstantiator implements EmbeddableInstantiator {
	@Override
	public Object instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactory) {
		final BigDecimal amount = valueAccess.getValue(0, BigDecimal.class);
		final Currency currency = valueAccess.getValue(1, Currency.class);

		if ( amount == null && currency == null ) {
			return null;
		}

		return new MonetaryAmount( amount, currency );
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return object instanceof MonetaryAmount;
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return MonetaryAmount.class.equals( object.getClass() );
	}
}
