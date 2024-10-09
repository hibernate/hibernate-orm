/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @since 7.0
 */
@Incubating
public abstract class AbstractSqmSetReturningFunctionDescriptor implements SqmSetReturningFunctionDescriptor {
	private final ArgumentsValidator argumentsValidator;
	private final SetReturningFunctionTypeResolver setReturningTypeResolver;
	private final FunctionArgumentTypeResolver functionArgumentTypeResolver;
	private final String name;

	public AbstractSqmSetReturningFunctionDescriptor(String name, SetReturningFunctionTypeResolver typeResolver) {
		this( name, null, typeResolver, null );
	}

	public AbstractSqmSetReturningFunctionDescriptor(
			String name,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver typeResolver) {
		this( name, argumentsValidator, typeResolver, null );
	}

	public AbstractSqmSetReturningFunctionDescriptor(
			String name,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver typeResolver,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver) {
		this.name = name;
		this.argumentsValidator = argumentsValidator == null
				? StandardArgumentsValidators.NONE
				: argumentsValidator;
		this.setReturningTypeResolver = typeResolver;
		this.functionArgumentTypeResolver = argumentTypeResolver == null
				? StandardFunctionArgumentTypeResolvers.NULL
				: argumentTypeResolver;
	}

	public String getName() {
		return name;
	}

	public String getSignature(String name) {
		return name + getArgumentListSignature();
	}

	@Override
	public ArgumentsValidator getArgumentsValidator() {
		return argumentsValidator;
	}

	public SetReturningFunctionTypeResolver getSetReturningTypeResolver() {
		return setReturningTypeResolver;
	}

	public FunctionArgumentTypeResolver getArgumentTypeResolver() {
		return functionArgumentTypeResolver;
	}

	public String getArgumentListSignature() {
		return argumentsValidator.getSignature();
	}

	@Override
	public final <T> SelfRenderingSqmSetReturningFunction<T> generateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine) {
		argumentsValidator.validate( arguments, getName(), queryEngine.getTypeConfiguration() );

		return generateSqmSetReturningFunctionExpression( arguments, queryEngine );
	}

	/**
	 * Return an SQM node or subtree representing an invocation of this function
	 * with the given arguments. This method may be overridden in the case of
	 * function descriptors that wish to customize creation of the node.
	 *
	 * @param arguments the arguments of the function invocation
	 */
	protected abstract <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine);
}
