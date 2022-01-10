/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A factory for SQM nodes representing invocations of a certain
 * named function. When a function names and arguments are
 * encountered in the HQL, a {@code SqmFunctionDescriptor} for
 * the given name is obtained from a {@link SqmFunctionRegistry},
 * and the {@link #generateSqmExpression} method is called with
 * the given argument SQM nodes to produce a subtree of SQM nodes
 * representing the function invocation.
 *
 * @author David Channon
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface SqmFunctionDescriptor {
	/**
	 * Instantiate this template with the given arguments and
	 * expected return type. This produces a tree of SQM nodes
	 * representing a tree of function invocations. This allows
	 * a single HQL function to be defined in terms of other
	 * predefined (database independent) HQL functions,
	 * simplifying the task of writing HQL functions which are
	 * portable between databases.
	 */
	<T> SelfRenderingSqmFunction<T> generateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration);

	/**
	 * Like {@link #generateSqmExpression(List, ReturnableType, QueryEngine, TypeConfiguration)}
	 * but also accepts a filter predicate. This method is intended for aggregate functions.
	 */
	default <T> SelfRenderingSqmFunction<T> generateAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Not an aggregate function!" );
	}

	/**
	 * Convenience for single argument
	 */
	default <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			SqmTypedNode<?> argument,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return generateSqmExpression(
				singletonList(argument),
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	/**
	 * Convenience for no arguments
	 */
	default <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return generateSqmExpression(
				emptyList(),
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	/**
	 * Will a call to the described function always include
	 * parentheses?
	 * <p>
	 * SqmFunctionTemplate is generally used for rendering of a function.
	 * However there are cases where Hibernate needs to consume a fragment
	 * and decide if a token represents a function name.  In cases where
	 * the token is followed by an open-paren we can safely assume the
	 * token is a function name.  However, if the next token is not an
	 * open-paren, the token can still represent a function provided that
	 * the function has a "no paren" form in the case of no arguments.  E.g.
	 * Many databases do not require parentheses on functions like
	 * `current_timestamp`, etc.  This method helps account for those
	 * cases.
	 * <p>
	 * Note that the most common case, by far, is that a function will always
	 * include the parentheses - therefore this return is defined as true by
	 * default.
	 *
	 * see Template#isFunction
	 */
	default boolean alwaysIncludesParentheses() {
		return true;
	}

	default String getSignature(String name) {
		return name;
	}

	default FunctionKind getFunctionKind() {
		return FunctionKind.NORMAL;
	}

	ArgumentsValidator getArgumentsValidator();
}
