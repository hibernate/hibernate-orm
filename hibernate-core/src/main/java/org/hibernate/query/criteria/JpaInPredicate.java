package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;

/**
 * @author Steve Ebersole
 */
public interface JpaInPredicate<T> extends JpaPredicate, CriteriaBuilder.In<T>  {
	/**
	 * Return the expression to be tested against the
	 * list of values.
	 * @return expression
	 */
	@Override
	@Nonnull
	JpaExpression<T> getExpression();

	/**
	 *  Add to list of values to be tested against.
	 *  @param value value
	 *  @return in predicate
	 */
	@Override
	@Nonnull
	JpaInPredicate<T> value(@Nonnull T value);

	/**
	 *  Add to list of values to be tested against.
	 *  @param value expression
	 *  @return in predicate
	 */
	@Nonnull
	JpaInPredicate<T> value(@Nonnull JpaExpression<? extends T> value);
}
