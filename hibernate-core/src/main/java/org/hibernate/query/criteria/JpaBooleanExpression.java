package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;

/// API extension to the JPA {@link BooleanExpression} contract
///
/// @author Steve Ebersole
public interface JpaBooleanExpression extends BooleanExpression, JpaComparableExpression<Boolean> {

	// Override methods to return JpaBooleanExpression for method chaining

	@Nonnull
	@Override
	JpaBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	JpaBooleanExpression coalesce(@Nonnull Boolean y);

	@Nonnull
	@Override
	JpaBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	JpaBooleanExpression nullif(@Nonnull Boolean y);
}
