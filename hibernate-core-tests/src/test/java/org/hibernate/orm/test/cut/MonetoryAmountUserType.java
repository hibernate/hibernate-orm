package org.hibernate.orm.test.cut;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nullable
	public Object getPropertyValue(@Nonnull MonetoryAmount component, int property) throws HibernateException {
		return property == 0 ? component.getAmount() : component.getCurrency();
	}

	@Override
	@Nullable
	public MonetoryAmount instantiate(@Nonnull ValueAccess valueAccess) {
		final BigDecimal value = valueAccess.getValue( 0, BigDecimal.class );
		final Currency currency = valueAccess.getValue( 1, Currency.class );

		if ( value == null && currency == null ) {
			return null;
		}
		return new MonetoryAmount( value, currency );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return MonetoryAmount.class;
	}

	@Override
	@Nonnull
	public Class returnedClass() {
		return MonetoryAmount.class;
	}

	@Override
	public boolean equals(@Nullable MonetoryAmount x, @Nullable MonetoryAmount y) throws HibernateException {
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
	public int hashCode(@Nonnull MonetoryAmount x) throws HibernateException {
		return x.getAmount().hashCode();
	}


	@Override
	@Nonnull
	public MonetoryAmount deepCopy(@Nonnull MonetoryAmount value) throws HibernateException {
		return new MonetoryAmount( value.getAmount(), value.getCurrency() );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@Nonnull
	public Serializable disassemble(@Nonnull MonetoryAmount value)
			throws HibernateException {
		return deepCopy( value );
	}

	@Override
	@Nonnull
	public MonetoryAmount assemble(@Nonnull Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return deepCopy( (MonetoryAmount) cached );
	}

	@Override
	@Nonnull
	public MonetoryAmount replace(
			@Nonnull MonetoryAmount original,
			@Nullable MonetoryAmount target,
			@Nullable Object owner)
			throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}

//	public static class MonetaryAmountEmbeddable {
//		private BigDecimal amount;
//		private Currency currency;
//	}

}
