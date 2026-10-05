package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.TextExpression;

/// API extension to the JPA {@link TextExpression} contract
///
/// @author Steve Ebersole
public interface JpaTextExpression extends TextExpression, JpaComparableExpression<String> {

	// Override methods to return JpaTextExpression for method chaining

	@Nonnull
	@Override
	JpaTextExpression coalesce(@Nonnull Expression<? extends String> y);

	@Nonnull
	@Override
	JpaTextExpression coalesce(@Nonnull String y);

	@Nonnull
	@Override
	JpaTextExpression nullif(@Nonnull Expression<? extends String> y);

	@Nonnull
	@Override
	JpaTextExpression nullif(@Nonnull String y);
}
