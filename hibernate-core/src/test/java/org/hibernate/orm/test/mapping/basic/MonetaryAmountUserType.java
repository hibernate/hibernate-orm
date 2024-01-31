/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Emmanuel Bernard
 */
public class MonetaryAmountUserType implements CompositeUserType<MonetaryAmount> {

	public static final MonetaryAmountUserType INSTANCE = new MonetaryAmountUserType();

	@Override
	public Object getPropertyValue(MonetaryAmount component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return component.getAmount();
			case 1:
				return component.getCurrency();
		}
		throw new HibernateException( "Illegal property index: " + property );
	}

	@Override
	public MonetaryAmount instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactory) {
		final BigDecimal amount = valueAccess.getValue(0, BigDecimal.class);
		final Currency currency = valueAccess.getValue(1, Currency.class);

		if ( amount == null && currency == null ) {
			return null;
		}

		return new MonetaryAmount( amount, currency );
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
		return true;
	}

	@Override
	public MonetaryAmount deepCopy(MonetaryAmount value) {
		return new MonetaryAmount( value.getAmount(), value.getCurrency() );
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
		return deepCopy( value );
	}

	@Override
	public MonetaryAmount assemble(Serializable cached, Object owner) throws HibernateException {
		return deepCopy( (MonetaryAmount) cached );
	}

	@Override
	public MonetaryAmount replace(MonetaryAmount original, MonetaryAmount target, Object owner) throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}

	@Override
	public int hashCode(MonetaryAmount x) throws HibernateException {
		return x.hashCode();
	}

	public static class MonetaryAmountEmbeddable {
		private BigDecimal amount;
		private Currency currency;
	}
}
