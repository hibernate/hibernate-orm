package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.TemporalExpression;

import java.time.temporal.Temporal;

/// API extension to the JPA {@link TemporalExpression} contract
///
/// @author Steve Ebersole
public interface JpaTemporalExpression<T extends Temporal & Comparable<? super T>>
		extends TemporalExpression<T>, JpaComparableExpression<T> {

	// Override methods to return JpaTemporalExpression for method chaining

	@Nonnull
	@Override
	JpaTemporalExpression<T> coalesce(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	JpaTemporalExpression<T> coalesce(@Nonnull T y);

	@Nonnull
	@Override
	JpaTemporalExpression<T> nullif(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	JpaTemporalExpression<T> nullif(@Nonnull T y);
}
