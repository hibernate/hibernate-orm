package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.ComparableExpression;
import jakarta.persistence.criteria.Expression;

/// API extension to the JPA {@link ComparableExpression} contract
///
/// @author Steve Ebersole
public interface JpaComparableExpression<C extends Comparable<? super C>> extends JpaExpression<C>, ComparableExpression<C> {
	// Override methods that have conflicting return types from both parent interfaces

	@Nonnull
	@Override
	JpaComparableExpression<C> coalesce(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	JpaComparableExpression<C> coalesce(@Nonnull C y);

	@Nonnull
	@Override
	JpaComparableExpression<C> nullif(@Nonnull Expression<? extends C> y);

	@Nonnull
	@Override
	JpaComparableExpression<C> nullif(@Nonnull C y);
}
