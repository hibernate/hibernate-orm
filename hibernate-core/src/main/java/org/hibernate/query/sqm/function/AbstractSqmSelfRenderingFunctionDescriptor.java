/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * @author Gavin King
 */
public abstract class AbstractSqmSelfRenderingFunctionDescriptor
		extends AbstractSqmFunctionDescriptor implements FunctionRenderer {

	private final FunctionKind functionKind;

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String name,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nullable FunctionReturnTypeResolver returnTypeResolver,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver, argumentTypeResolver );
		this.functionKind = FunctionKind.NORMAL;
	}

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String name,
			FunctionKind functionKind,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nullable FunctionReturnTypeResolver returnTypeResolver,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver, argumentTypeResolver );
		this.functionKind = functionKind;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, walker );
	}

	@Override
	public FunctionKind getFunctionKind() {
		return functionKind;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return switch (functionKind) {
			case ORDERED_SET_AGGREGATE ->
					generateOrderedSetAggregateSqmExpression(
							arguments,
							null,
							null,
							impliedResultType,
							queryEngine
					);
			case AGGREGATE ->
					generateAggregateSqmExpression(
							arguments,
							null,
							impliedResultType,
							queryEngine
					);
			case WINDOW ->
					generateWindowSqmExpression(
							arguments,
							null,
							null,
							null,
							impliedResultType,
							queryEngine
					);
			default ->
					new SelfRenderingSqmFunction<>(
							this,
							this,
							arguments,
							impliedResultType,
							getArgumentsValidator(),
							getReturnTypeResolver(),
							queryEngine.getCriteriaBuilder(),
							getName()
					);
		};
	}

	@Override
	public <T> SelfRenderingSqmAggregateFunction<T> generateSqmAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		if ( functionKind != FunctionKind.AGGREGATE ) {
			throw new UnsupportedOperationException( "The function " + getName() + " is not an aggregate function" );
		}
		return new SelfRenderingSqmAggregateFunction<>(
				this,
				this,
				arguments,
				filter,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public <T> SelfRenderingSqmOrderedSetAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		if ( functionKind != FunctionKind.ORDERED_SET_AGGREGATE ) {
			throw new UnsupportedOperationException( "The function " + getName() + " is not an ordered set-aggregate function" );
		}
		return new SelfRenderingSqmOrderedSetAggregateFunction<>(
				this,
				this,
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	protected <T> SelfRenderingSqmWindowFunction<T> generateSqmWindowFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		if ( functionKind != FunctionKind.WINDOW ) {
			throw new UnsupportedOperationException( "The function " + getName() + " is not a window function" );
		}
		return new SelfRenderingSqmWindowFunction<>(
				this,
				this,
				arguments,
				filter,
				respectNulls,
				fromFirst,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	public static boolean filterClauseSupported(SqlAstTranslator<?> translator) {
		return translator.getSessionFactory().getJdbcServices().getDialect().supportsFilterClause();
	}
}
