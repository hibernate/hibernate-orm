/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Gavin King
 */
public class MonetoryAmountUserType implements CompositeUserType<MonetoryAmount> {

	@Override
	public Object getPropertyValue(MonetoryAmount component, int property) throws HibernateException {
		return property == 0 ? component.getAmount() : component.getCurrency();
	}

	@Override
	public MonetoryAmount instantiate(ValueAccess valueAccess) {
		final BigDecimal value = valueAccess.getValue( 0, BigDecimal.class );
		final Currency currency = valueAccess.getValue( 1, Currency.class );

		if ( value == null && currency == null ) {
			return null;
		}
		return new MonetoryAmount( value, currency );
	}

	@Override
	public Class<?> embeddable() {
		return MonetoryAmount.class;
	}

	@Override
	public Class returnedClass() {
		return MonetoryAmount.class;
	}

	@Override
	public boolean equals(MonetoryAmount x, MonetoryAmount y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.getAmount().equals( y.getAmount() ) &&
				x.getCurrency().equals( y.getCurrency() );
	}

	@Override
	public int hashCode(MonetoryAmount x) throws HibernateException {
		return x.getAmount().hashCode();
	}


	@Override
	public MonetoryAmount deepCopy(MonetoryAmount value) throws HibernateException {
		return new MonetoryAmount( value.getAmount(), value.getCurrency() );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(MonetoryAmount value)
			throws HibernateException {
		return deepCopy( value );
	}

	@Override
	public MonetoryAmount assemble(Serializable cached, Object owner)
			throws HibernateException {
		return deepCopy( (MonetoryAmount) cached );
	}

	@Override
	public MonetoryAmount replace(MonetoryAmount original, MonetoryAmount target, Object owner)
			throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}

//	public static class MonetaryAmountEmbeddable {
//		private BigDecimal amount;
//		private Currency currency;
//	}

}
