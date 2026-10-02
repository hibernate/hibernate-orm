package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

/**
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaFunctionFrom<O, T> extends JpaFrom<O, T> {

	/**
	 * The function for this from node.
	 */
	@Nonnull
	JpaSetReturningFunction<T> getFunction();

	/**
	 * The expression referring to an iteration variable, indexing the rows produced by the function.
	 * This is the equivalent of the SQL {@code with ordinality} clause.
	 */
	@Nonnull
	JpaExpression<Long> index();

}
