/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.select.SqmOrderByClause;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Produces SQM nodes for invocations of a named HQL function.
///
/// Hibernate obtains a descriptor from the [SqmFunctionRegistry] and invokes
/// [#generateSqmExpression] with the SQM arguments and implied result type. A
/// custom descriptor may desugar the invocation into other SQM nodes or return
/// a [SelfRenderingSqmFunction] which owns SQL AST rendering.
///
/// Prefer registry builders for simple named or pattern functions. Implement a
/// descriptor when custom validation, SQM generation, type inference, or SQL
/// rendering is required, and register it during function bootstrap. A
/// registered descriptor must be safe for reuse and must not retain the
/// boot-scoped contribution callback or mutable registry.
///
/// @see SqmFunctionRegistry#register(String, SqmFunctionDescriptor)
/// @see SqmFunctionRegistry#wrapInJdbcEscape(String, SqmFunctionDescriptor)
/// @see org.hibernate.cfg.Configuration#addSqlFunction(String, SqmFunctionDescriptor)
/// @see org.hibernate.boot.MetadataBuilder#applySqlFunction(String, SqmFunctionDescriptor)
/// @see org.hibernate.boot.SessionFactoryBuilder#applySqlFunction(String, SqmFunctionDescriptor)
/// @see org.hibernate.boot.model.FunctionContributor
/// @see org.hibernate.dialect.Dialect#initializeFunctionRegistry(org.hibernate.boot.model.FunctionContributions)
///
/// @author David Channon
/// @author Steve Ebersole
/// @author Gavin King
@SPI({ USE, IMPLEMENT, SUPPLY })
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
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine);

	/**
	 * Like {@link #generateSqmExpression(List, ReturnableType, QueryEngine)},
	 * but also accepts a {@code filter} predicate.
	 * <p>
	 * This method is intended for aggregate functions.
	 */
	default <T> SelfRenderingSqmFunction<T> generateAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		throw new UnsupportedOperationException( "Not an aggregate function" );
	}

	/**
	 * Like {@link #generateSqmExpression(List, ReturnableType, QueryEngine)},
	 * but also accepts a {@code filter} predicate and an {@code order by} clause.
	 * <p>
	 * This method is intended for ordered set aggregate functions.
	 */
	default <T> SelfRenderingSqmFunction<T> generateOrderedSetAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		throw new UnsupportedOperationException( "Not an ordered set aggregate function" );
	}

	/**
	 * Like {@link #generateSqmExpression(List, ReturnableType, QueryEngine)}
	 * but also accepts a {@code filter} predicate.
	 * <p>
	 * This method is intended for window functions.
	 */
	default <T> SelfRenderingSqmFunction<T> generateWindowSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		throw new UnsupportedOperationException( "Not a window function" );
	}

	/**
	 * Convenience for a single argument.
	 */
	default <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			SqmTypedNode<?> argument,
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return generateSqmExpression(
				singletonList(argument),
				impliedResultType,
				queryEngine
		);
	}

	/**
	 * Convenience for no arguments.
	 */
	default <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return generateSqmExpression(
				emptyList(),
				impliedResultType,
				queryEngine
		);
	}

	/**
	 * Will a call to the described function always include parentheses?
	 * <p>
	 * Instances of this interface are usually used for rendering of functions.
	 * However, there are cases where Hibernate needs to consume a fragment
	 * and decide if a token represents a function name.  In cases where the
	 * token is followed by an opening parenthesis, we can safely assume the
	 * token is a function name. Bur if the next token is not an opening
	 * parenthesis, the token might still represent a function if the function
	 * has a "no paren" form in the case of no arguments.
	 * <p>
	 * For example, many databases do not require parentheses for functions
	 * like {@code current_timestamp} and friends. This method helps account
	 * for those cases.
	 *
	 * @apiNote The most common case, by far, is that a function call requires
	 *          the parentheses. So this method returns true by default.
	 *
	 * @return {@code true} by default
	 */
	default boolean alwaysIncludesParentheses() {
		return true;
	}

	/**
	 * Used only for pretty-printing the function signature in the LOG.
	 *
	 * @param name the function name
	 * @return the signature of the function
	 */
	default String getSignature(String name) {
		return name;
	}

	/**
	 * What sort of function is this?
	 *
	 * @return {@link FunctionKind#NORMAL} by default
	 */
	default FunctionKind getFunctionKind() {
		return FunctionKind.NORMAL;
	}

	/**
	 * The object responsible for validating arguments of the function.
	 *
	 * @return an instance of {@link ArgumentsValidator}
	 */
	ArgumentsValidator getArgumentsValidator();

	/**
	 * Whether the function renders as a predicate.
	 *
	 * @since 7.0
	 */
	default boolean isPredicate() {
		return false;
	}
}
