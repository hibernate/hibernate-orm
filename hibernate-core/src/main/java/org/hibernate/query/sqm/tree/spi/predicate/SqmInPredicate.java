package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nonnull;

import org.hibernate.query.criteria.JpaInPredicate;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmInPredicate<T> extends SqmNegatablePredicate, JpaInPredicate<T> {
	@Nonnull
	SqmExpression<T> getTestExpression();
}
