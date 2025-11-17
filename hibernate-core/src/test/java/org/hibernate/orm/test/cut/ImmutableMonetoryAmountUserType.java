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

public class ImmutableMonetoryAmountUserType implements CompositeUserType<ImmutableMonetoryAmount> {

	@Override
	public Object getPropertyValue(ImmutableMonetoryAmount component, int property) throws HibernateException {
		return property == 0 ? component.getAmount() : component.getCurrency();
	}

	@Override
	public ImmutableMonetoryAmount instantiate(ValueAccess valueAccess) {
		final BigDecimal value = valueAccess.getValue( 0, BigDecimal.class );
		final Currency currency = valueAccess.getValue( 1, Currency.class );

		if ( value == null && currency == null ) {
			return null;
		}
		return new ImmutableMonetoryAmount( value, currency );
	}

	@Override
	public Class<?> embeddable() {
		return ImmutableMonetoryAmount.class;
	}

	@Override
	public Class<ImmutableMonetoryAmount> returnedClass() {
		return ImmutableMonetoryAmount.class;
	}

	@Override
	public boolean equals(ImmutableMonetoryAmount x, ImmutableMonetoryAmount y) throws HibernateException {
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
	public int hashCode(ImmutableMonetoryAmount x) throws HibernateException {
		return x.getAmount().hashCode();
	}


	@Override
	public ImmutableMonetoryAmount deepCopy(ImmutableMonetoryAmount value) throws HibernateException {
		return new ImmutableMonetoryAmount( value.getAmount(), value.getCurrency() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(ImmutableMonetoryAmount value)
			throws HibernateException {
		return deepCopy( value );
	}

	@Override
	public ImmutableMonetoryAmount assemble(Serializable cached, Object owner)
			throws HibernateException {
		return deepCopy( (ImmutableMonetoryAmount) cached );
	}

	@Override
	public ImmutableMonetoryAmount replace(
			ImmutableMonetoryAmount original,
			ImmutableMonetoryAmount target,
			Object owner)
			throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}


}
