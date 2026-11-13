package org.hibernate.orm.test.cut;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class ImmutableMonetoryAmountUserType implements CompositeUserType<ImmutableMonetoryAmount> {

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull ImmutableMonetoryAmount component, int property) throws HibernateException {
		return property == 0 ? component.getAmount() : component.getCurrency();
	}

	@Override
	@Nullable
	public ImmutableMonetoryAmount instantiate(@Nonnull ValueAccess valueAccess) {
		final BigDecimal value = valueAccess.getValue( 0, BigDecimal.class );
		final Currency currency = valueAccess.getValue( 1, Currency.class );

		if ( value == null && currency == null ) {
			return null;
		}
		return new ImmutableMonetoryAmount( value, currency );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return ImmutableMonetoryAmount.class;
	}

	@Override
	@Nonnull
	public Class<ImmutableMonetoryAmount> returnedClass() {
		return ImmutableMonetoryAmount.class;
	}

	@Override
	public boolean equals(
			@Nullable ImmutableMonetoryAmount x,
			@Nullable ImmutableMonetoryAmount y) throws HibernateException {
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
	public int hashCode(@Nonnull ImmutableMonetoryAmount x) throws HibernateException {
		return x.getAmount().hashCode();
	}


	@Override
	@Nonnull
	public ImmutableMonetoryAmount deepCopy(@Nonnull ImmutableMonetoryAmount value) throws HibernateException {
		return new ImmutableMonetoryAmount( value.getAmount(), value.getCurrency() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nonnull
	public Serializable disassemble(@Nonnull ImmutableMonetoryAmount value)
			throws HibernateException {
		return deepCopy( value );
	}

	@Override
	@Nonnull
	public ImmutableMonetoryAmount assemble(@Nonnull Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return deepCopy( (ImmutableMonetoryAmount) cached );
	}

	@Override
	@Nonnull
	public ImmutableMonetoryAmount replace(
			@Nonnull ImmutableMonetoryAmount original,
			@Nullable ImmutableMonetoryAmount target,
			@Nullable Object owner)
			throws HibernateException {
		return deepCopy( original ); //TODO: improve
	}


}
