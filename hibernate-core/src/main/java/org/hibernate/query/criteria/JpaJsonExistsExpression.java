package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code json_exists} function.
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaJsonExistsExpression extends JpaExpression<Boolean>, JpaJsonExistsNode {

	/**
	 * Passes the given {@link Expression} as value for the parameter with the given name in the JSON path.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaJsonExistsExpression passing(@Nonnull String parameterName, @Nonnull Expression<?> expression);

	/**
	 * Use the unspecified JSON error behavior.
	 */
	@Nonnull
	@Override
	JpaJsonExistsExpression unspecifiedOnError();
	/**
	 * Use the JSON error behavior that raises an error.
	 */
	@Nonnull
	@Override
	JpaJsonExistsExpression errorOnError();
	/**
	 * Use the JSON error behavior that returns true.
	 */
	@Nonnull
	@Override
	JpaJsonExistsExpression trueOnError();
	/**
	 * Use the JSON error behavior that returns false.
	 */
	@Nonnull
	@Override
	JpaJsonExistsExpression falseOnError();
}
