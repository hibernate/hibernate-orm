/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * The base contract for any kind of expression node in the SQM tree.
 * An expression might be a reference to an attribute, a literal, etc.
 *
 * @author Steve Ebersole
 */
public interface SqmExpression {
	// todo : contemplate way to incorporate DomainReference into expressions in place of the removed types.

	/**
	 * Obtain reference to the expression's type
	 *
	 * @return The expression's type.
	 */
	ExpressableType getExpressionType();

	/**
	 * Obtain reference to the type, or {@code null}, for this expression that can be used
	 * to infer the "implied type" of related expressions. Not all expressions can act as the
	 * source of an inferred type, in which case the method would return {@code null}.
	 *
	 * @return The inferable type
	 *
	 * @see ImpliedTypeSqmExpression#impliedType
	 */
	ExpressableType getInferableType();

	/**
	 * Visitation method
	 *
	 * @param walker The visitation walker.
	 * @param <T> The expected result type.
	 *
	 * @return The visitation result
	 */
	<T> T accept(SemanticQueryWalker<T> walker);

	String asLoggableText();
}
