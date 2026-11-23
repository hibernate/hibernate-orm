package org.hibernate.orm.test.sql.hand;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nullable
	public Object getPropertyValue(@Nonnull MonetaryAmount component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.getCurrency();
			case 1 -> component.getValue();
			default -> throw new HibernateException( "Illegal property index: " + property );
		};
	}

	@Override
	@Nullable
	public MonetaryAmount instantiate(@Nonnull ValueAccess valueAccess) {
		final BigDecimal value = valueAccess.getValue(0, BigDecimal.class);
		final Currency currency = valueAccess.getValue(1, Currency.class);

		if ( value == null && currency == null ) {
			return null;
		}
		return new MonetaryAmount( value, currency );
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
		return false;
	}

	@Override
	@Nonnull
	public MonetaryAmount deepCopy(@Nonnull MonetaryAmount value) {
		return value; // MonetaryAmount is immutable
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
	@Nonnull
	public Serializable disassemble(@Nonnull MonetaryAmount value) throws HibernateException {
		return value;
	}

	@Override
	@Nonnull
	public MonetaryAmount assemble(@Nonnull Serializable cached, @Nullable Object owner) throws HibernateException {
		return (MonetaryAmount) cached;
	}

	@Override
	@Nonnull
	public MonetaryAmount replace(
			@Nonnull MonetaryAmount original,
			@Nullable MonetaryAmount target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(@Nonnull MonetaryAmount x) throws HibernateException {
		return x.hashCode();
	}

	public static class MonetaryAmountEmbeddable {
		private BigDecimal value;
		private Currency currency;
	}
}
