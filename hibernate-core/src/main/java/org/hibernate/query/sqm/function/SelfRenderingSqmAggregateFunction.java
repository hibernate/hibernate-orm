/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmAggregateFunction;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Christian Beikov
 */
public class SelfRenderingSqmAggregateFunction<T> extends SelfRenderingSqmFunction<T>
		implements SqmAggregateFunction<T> {

	private final SqmPredicate filter;

	public SelfRenderingSqmAggregateFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( descriptor, renderer, arguments, impliedResultType, argumentsValidator, returnTypeResolver, nodeBuilder, name );
		this.filter = filter;
	}

	@Override
	public SelfRenderingSqmAggregateFunction<T> copy(SqmCopyContext context) {
		final SelfRenderingSqmAggregateFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		else {
			final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
			for ( SqmTypedNode<?> argument : getArguments() ) {
				arguments.add( argument.copy( context ) );
			}
			final SelfRenderingSqmAggregateFunction<T> expression = context.registerCopy(
					this,
					new SelfRenderingSqmAggregateFunction<>(
							getFunctionDescriptor(),
							getFunctionRenderer(),
							arguments,
							filter == null ? null : filter.copy( context ),
							getImpliedResultType(),
							getArgumentsValidator(),
							getReturnTypeResolver(),
							nodeBuilder(),
							getFunctionName()
					)
			);
			copyTo( expression, context );
			return expression;
		}
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		final ReturnableType<?> resultType = resolveResultType( walker );

		List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		ArgumentsValidator argumentsValidator = getArgumentsValidator();
		if ( argumentsValidator != null ) {
			argumentsValidator.validateSqlTypes( arguments, getFunctionName() );
		}
		return new SelfRenderingAggregateFunctionSqlAstExpression<>(
				getFunctionName(),
				getFunctionRenderer(),
				arguments,
				filter == null ? null : walker.visitNestedTopLevelPredicate( filter ),
				resultType,
				getMappingModelExpressible( walker, resultType, arguments )
		);
	}

	@Override
	public SqmPredicate getFilter() {
		return filter;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		hql.append( getFunctionName() );
		hql.append( '(' );
		int i = 1;
		if ( arguments.get( 0 ) instanceof SqmDistinct<?> ) {
			arguments.get( 0 ).appendHqlString( hql, context );
			if ( arguments.size() > 1 ) {
				hql.append( ' ' );
				arguments.get( 1 ).appendHqlString( hql, context );
				i = 2;
			}
		}
		for ( ; i < arguments.size(); i++ ) {
			hql.append(", ");
			arguments.get( i ).appendHqlString( hql, context );
		}

		hql.append( ')' );
		if ( filter != null ) {
			hql.append( " filter (where " );
			filter.appendHqlString( hql, context );
			hql.append( ')' );
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		SelfRenderingSqmAggregateFunction<?> that = (SelfRenderingSqmAggregateFunction<?>) o;
		return Objects.equals( filter, that.filter );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Objects.hashCode( filter );
		return result;
	}
}
