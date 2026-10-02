package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;

import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;

/**
 * A SQM aggregate function.
 *
 * @param <T> The Java type of the expression
 *
 * @author Christian Beikov
 */
public interface SqmAggregateFunction<T> extends JpaFunction<T>, SqmExpression<T> {

	@Nullable
	SqmPredicate getFilter();
}
