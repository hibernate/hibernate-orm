package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaTemporalExpression;

import java.time.temporal.Temporal;

/**
 * @author Steve Ebersole
 */
public interface SqmTemporalExpression<T extends Temporal & Comparable<? super T>>
		extends SqmComparableExpression<T>, JpaTemporalExpression<T> {
	@Nonnull
	@Override
	SqmTemporalExpression<T> coalesce(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	SqmTemporalExpression<T> coalesce(@Nonnull T y);

	@Nonnull
	@Override
	SqmTemporalExpression<T> nullif(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	SqmTemporalExpression<T> nullif(@Nonnull T y);
}
