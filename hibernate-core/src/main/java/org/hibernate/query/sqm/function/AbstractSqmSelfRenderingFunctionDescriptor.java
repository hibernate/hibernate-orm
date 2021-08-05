/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * @author Gavin King
 */
public abstract class AbstractSqmSelfRenderingFunctionDescriptor
		extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {

	private final FunctionKind functionKind;

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String name,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver );
		this.functionKind = FunctionKind.NORMAL;
	}

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String name,
			FunctionKind functionKind,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver );
		this.functionKind = functionKind;
	}

	@Override
	public FunctionKind getFunctionKind() {
		return functionKind;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		if ( functionKind == FunctionKind.AGGREGATE ) {
			return generateAggregateSqmExpression( arguments, null, impliedResultType, queryEngine, typeConfiguration );
		}
		return new SelfRenderingSqmFunction<>(
				this,
				this::render,
				arguments,
				impliedResultType,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public <T> SelfRenderingSqmAggregateFunction<T> generateSqmAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		if ( functionKind != FunctionKind.AGGREGATE ) {
			throw new UnsupportedOperationException( "The function " + getName() + " is not an aggregate function!" );
		}
		return new SelfRenderingSqmAggregateFunction<>(
				this,
				this,
				arguments,
				filter,
				impliedResultType,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	/**
	 * Must be overridden by subclasses
	 */
	public abstract void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker);

	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, walker );
	}
}
