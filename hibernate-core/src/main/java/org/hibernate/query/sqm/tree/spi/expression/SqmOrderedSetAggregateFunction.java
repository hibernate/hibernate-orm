package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;

import org.hibernate.query.sqm.tree.spi.select.SqmOrderByClause;

/**
 * A SQM ordered set-aggregate function.
 *
 * @param <T> The Java type of the expression
 *
 * @author Christian Beikov
 */
public interface SqmOrderedSetAggregateFunction<T> extends SqmAggregateFunction<T> {

	@Nullable
	SqmOrderByClause getWithinGroup();
}
