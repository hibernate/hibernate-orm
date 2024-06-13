/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A function that dynamically dispatches to other functions,
 * depending on which function validates successfully first.
 * This can be used for overload implementations.
 *
 * @since 6.6
 */
public class DynamicDispatchFunction implements SqmFunctionDescriptor, ArgumentsValidator {
	private final SqmFunctionRegistry functionRegistry;
	private final String[] functionNames;
	private final FunctionKind functionKind;

	public DynamicDispatchFunction(SqmFunctionRegistry functionRegistry, String... functionNames) {
		this.functionRegistry = functionRegistry;
		this.functionNames = functionNames;

		FunctionKind functionKind = null;
		// Sanity check
		for ( String overload : functionNames ) {
			final SqmFunctionDescriptor functionDescriptor = functionRegistry.findFunctionDescriptor( overload );
			if ( functionDescriptor == null ) {
				throw new IllegalArgumentException( "No function registered under the name '" + overload + "'" );
			}
			if ( functionKind == null ) {
				functionKind = functionDescriptor.getFunctionKind();
			}
			else if ( functionKind != functionDescriptor.getFunctionKind() ) {
				throw new IllegalArgumentException( "Function has function kind " + functionDescriptor.getFunctionKind() + ", but other overloads have " + functionKind + ". An overloaded function needs a single function kind." );
			}
		}
		this.functionKind = functionKind;
	}

	@Override
	public FunctionKind getFunctionKind() {
		return functionKind;
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final SqmFunctionDescriptor functionDescriptor = validateGetFunction(
				arguments,
				queryEngine.getTypeConfiguration()
		);
		return functionDescriptor.generateSqmExpression( arguments, impliedResultType, queryEngine );
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final SqmFunctionDescriptor functionDescriptor = validateGetFunction(
				arguments,
				queryEngine.getTypeConfiguration()
		);
		return functionDescriptor.generateAggregateSqmExpression(
				arguments,
				filter,
				impliedResultType,
				queryEngine
		);
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateOrderedSetAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final SqmFunctionDescriptor functionDescriptor = validateGetFunction(
				arguments,
				queryEngine.getTypeConfiguration()
		);
		return functionDescriptor.generateOrderedSetAggregateSqmExpression(
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				queryEngine
		);
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateWindowSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final SqmFunctionDescriptor functionDescriptor = validateGetFunction(
				arguments,
				queryEngine.getTypeConfiguration()
		);
		return functionDescriptor.generateWindowSqmExpression(
				arguments,
				filter,
				respectNulls,
				fromFirst,
				impliedResultType,
				queryEngine
		);
	}

	@Override
	public ArgumentsValidator getArgumentsValidator() {
		return this;
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		validateGetFunction( arguments, typeConfiguration );
	}

	private SqmFunctionDescriptor validateGetFunction(
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		RuntimeException exception = null;
		for ( String overload : functionNames ) {
			final SqmFunctionDescriptor functionDescriptor = functionRegistry.findFunctionDescriptor( overload );
			if ( functionDescriptor == null ) {
				throw new IllegalArgumentException( "No function registered under the name '" + overload + "'" );
			}
			try {
				functionDescriptor.getArgumentsValidator().validate( arguments, overload, typeConfiguration );
				return functionDescriptor;
			}
			catch (RuntimeException ex) {
				if ( exception == null ) {
					exception = ex;
				}
				else {
					exception.addSuppressed( ex );
				}
			}
		}
		throw exception;
	}
}
