/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A factory for SQM nodes representing invocations of a certain
 * named function.
 * <p>
 * When a function call is encountered in the text of an HQL query,
 * a {@code SqmFunctionDescriptor} for the given name is obtained
 * from the {@link SqmFunctionRegistry}, and the
 * {@link #generateSqmExpression} method is called with SQM nodes
 * representing the invocation arguments. It is the responsibility
 * of the {@code SqmFunctionDescriptor} to produce a subtree of SQM
 * nodes representing the function invocation.
 * <p>
 * The resulting subtree might be quite complex, since the
 * {@code SqmFunctionDescriptor} is permitted to perform syntactic
 * de-sugaring. On the other hand, {@link #generateSqmExpression}
 * returns {@link SelfRenderingSqmFunction}, which is an object
 * that is permitted to take over the logic of producing the
 * SQL AST subtree, so de-sugaring may also be performed there.
 * <p>
 * User-written function descriptors may be contributed via a
 * {@link org.hibernate.boot.model.FunctionContributor} or by
 * calling {@link org.hibernate.cfg.Configuration#addSqlFunction}.
 * The {@link SqmFunctionRegistry} exposes methods which simplify
 * the definition of a function, including
 * {@link SqmFunctionRegistry#namedDescriptorBuilder(String)} and
 * {@link SqmFunctionRegistry#patternAggregateDescriptorBuilder(String, String)}.
 * <p>
 * For example, this code registers a function named {@code prefixes()}:
 * <pre>
 * Configuration config = ... ;
 * config.addSqlFunction("prefixes",
 *         new SqmFunctionDescriptor() {
 *             &#064;Override
 *             public &lt;T&gt; SelfRenderingSqmFunction&lt;T&gt; generateSqmExpression(
 *                     List&lt;? extends SqmTypedNode&lt;?&gt;&gt; arguments,
 *                     ReturnableType&lt;T&gt; impliedResultType,
 *                     QueryEngine queryEngine) {
 *                 final SqmFunctionRegistry registry = queryEngine.getSqmFunctionRegistry();
 *                 final TypeConfiguration types = queryEngine.getTypeConfiguration();
 *                 return registry.patternDescriptorBuilder("prefix", "(left(?1, character_length(?2)) = ?2)" )
 *                         .setExactArgumentCount(2)
 *                         .setParameterTypes(FunctionParameterType.STRING, FunctionParameterType.STRING)
 *                         .setInvariantType(types.standardBasicTypeForJavaType(Boolean.class))
 *                         .descriptor()
 *                         .generateSqmExpression(arguments, impliedResultType, queryEngine);
 *             }
 *
 *             &#064;Override
 *             public ArgumentsValidator getArgumentsValidator() {
 *                 return new ArgumentTypesValidator(
 *                         StandardArgumentsValidators.exactly(2),
 *                         FunctionParameterType.STRING, FunctionParameterType.STRING
 *                 );
 *             }
 *         }
 * );
 * </pre>
 * The function may be called like this: {@code prefixes('Hibernate',book.title)}.
 *
 * @see org.hibernate.query.sqm.function.SqmFunctionRegistry
 * @see org.hibernate.cfg.Configuration#addSqlFunction
 * @see org.hibernate.boot.MetadataBuilder#applySqlFunction
 * @see org.hibernate.boot.model.FunctionContributor
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
