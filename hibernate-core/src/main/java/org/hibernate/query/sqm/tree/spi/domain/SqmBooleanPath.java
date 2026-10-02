package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.spi.expression.SqmBooleanExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanPath extends SqmPath<Boolean>, SqmBooleanExpression {
	@Nonnull
	@Override
	SqmBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	SqmBooleanExpression coalesce(@Nonnull Boolean y);

	@Nonnull
	@Override
	SqmBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	SqmBooleanExpression nullif(@Nonnull Boolean y);
}
