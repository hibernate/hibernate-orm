package org.hibernate.orm.test.mapping.basic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Emmanuel Bernard
 */
public class MonetaryAmountUserType implements CompositeUserType<MonetaryAmount> {

	public static final MonetaryAmountUserType INSTANCE = new MonetaryAmountUserType();

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull MonetaryAmount component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.getAmount();
			case 1 -> component.getCurrency();
			default -> throw new HibernateException( "Illegal property index: " + property );
		};
	}

	@Override
	@Nullable
	public MonetaryAmount instantiate(@Nonnull ValueAccess valueAccess) {
		final BigDecimal amount = valueAccess.getValue(0, BigDecimal.class);
		final Currency currency = valueAccess.getValue(1, Currency.class);

		if ( amount == null && currency == null ) {
			return null;
		}

		return new MonetaryAmount( amount, currency );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return MonetaryAmountEmbeddable.class;
	}

	@Override
	@Nonnull
	public Class<MonetaryAmount> returnedClass() {
		return MonetaryAmount.class;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@Nullable
	public MonetaryAmount deepCopy(@Nullable MonetaryAmount value) {
		return value == null ? null : new MonetaryAmount( value.getAmount(), value.getCurrency() );
	}

	@Override
	public boolean equals(@Nullable MonetaryAmount x, @Nullable MonetaryAmount y) {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable MonetaryAmount value) throws HibernateException {
		return deepCopy( value );
	}

	@Override
	@Nullable
	public MonetaryAmount assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return deepCopy( (MonetaryAmount) cached );
	}

	@Override
	@Nullable
	public MonetaryAmount replace(
			@Nullable MonetaryAmount original,
			@Nullable MonetaryAmount target,
			@Nullable Object owner) throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}

	@Override
	public int hashCode(@Nonnull MonetaryAmount x) throws HibernateException {
		return x.hashCode();
	}

	public static class MonetaryAmountEmbeddable {
		private BigDecimal amount;
		private Currency currency;
	}
}
