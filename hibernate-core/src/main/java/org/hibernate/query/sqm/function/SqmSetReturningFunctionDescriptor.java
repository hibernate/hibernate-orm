/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;

/**
 * A factory for SQM nodes representing invocations of a certain
 * named set-returning function.
 * <p>
 * When a function call is encountered in the text of an HQL query,
 * a {@code SqmSetReturningFunctionDescriptor} for the given name is obtained
 * from the {@link SqmFunctionRegistry}, and the
 * {@link #generateSqmExpression} method is called with SQM nodes
 * representing the invocation arguments. It is the responsibility
 * of the {@code SqmSetReturningFunctionDescriptor} to produce a subtree of SQM
 * nodes representing the function invocation.
 * <p>
 * The resulting subtree might be quite complex, since the
 * {@code SqmSetReturningFunctionDescriptor} is permitted to perform syntactic
 * de-sugaring. On the other hand, {@link #generateSqmExpression}
 * returns {@link SelfRenderingSqmSetReturningFunction}, which is an object
 * that is permitted to take over the logic of producing the
 * SQL AST subtree, so de-sugaring may also be performed there.
 * <p>
 * User-written function descriptors may be contributed via a
 * {@link org.hibernate.boot.model.FunctionContributor}.
 * The {@link SqmFunctionRegistry} exposes methods which simplify
 * the definition of a function, including
 * {@link SqmFunctionRegistry#namedSetReturningDescriptorBuilder(String, SetReturningFunctionTypeResolver)}.
 *
 * @see SqmFunctionRegistry
 * @see org.hibernate.boot.model.FunctionContributor
 *
 * @since 7.0
 */
@Incubating
public interface SqmSetReturningFunctionDescriptor {

	/**
	 * Instantiate this template with the given arguments and.
	 * This produces a tree of SQM nodes
	 * representing a tree of function invocations. This allows
	 * a single HQL function to be defined in terms of other
	 * predefined (database independent) HQL functions,
	 * simplifying the task of writing HQL functions which are
	 * portable between databases.
	 *
	 */
	<T> SelfRenderingSqmSetReturningFunction<T> generateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine);

	/**
	 * Used only for pretty-printing the function signature in the log.
	 *
	 * @param name the function name
	 * @return the signature of the function
	 */
	default String getSignature(String name) {
		return name;
	}

	/**
	 * The object responsible for validating arguments of the function.
	 *
	 * @return an instance of {@link ArgumentsValidator}
	 */
	ArgumentsValidator getArgumentsValidator();

}
