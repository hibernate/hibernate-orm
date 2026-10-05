package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.NumericExpression;

/// API extension to the JPA {@link NumericExpression} contract
///
/// @author Steve Ebersole
public interface JpaNumericExpression<N extends Number & Comparable<N>>
		extends NumericExpression<N>, JpaComparableExpression<N> {

	// Override methods to return JpaNumericExpression for method chaining

	@Nonnull
	@Override
	JpaNumericExpression<N> coalesce(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	JpaNumericExpression<N> coalesce(@Nonnull N y);

	@Nonnull
	@Override
	JpaNumericExpression<N> nullif(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	JpaNumericExpression<N> nullif(@Nonnull N y);
}
