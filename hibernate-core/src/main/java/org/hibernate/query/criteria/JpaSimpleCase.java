package org.hibernate.query.criteria;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaSimpleCase<C,R> extends JpaExpression<R>, CriteriaBuilder.SimpleCase<C,R> {
	/**
	 * Return the expression tested by this simple case expression.
	 */
	@Nonnull
	@Override
	JpaExpression<C> getExpression();

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nullable C condition, @Nullable R result);

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nullable C condition, @Nonnull Expression<? extends R> result);

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nonnull Expression<? extends C> condition, @Nullable R result);

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nonnull Expression<? extends C> condition, @Nonnull Expression<? extends R> result);

	/**
	 * Set the otherwise result of this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C,R> otherwise(@Nullable R result);

	/**
	 * Set the otherwise result of this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C,R> otherwise(@Nonnull Expression<? extends R> result);
}
