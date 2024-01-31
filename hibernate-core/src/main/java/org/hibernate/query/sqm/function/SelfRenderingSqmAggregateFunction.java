/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
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

	/**
	 * @deprecated Use {@link #SelfRenderingSqmAggregateFunction(SqmFunctionDescriptor, FunctionRenderer, List, SqmPredicate, ReturnableType, ArgumentsValidator, FunctionReturnTypeResolver, NodeBuilder, String)} instead
	 */
	@Deprecated(forRemoval = true)
	public SelfRenderingSqmAggregateFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( descriptor, renderingSupport, arguments, impliedResultType, argumentsValidator, returnTypeResolver, nodeBuilder, name );
		this.filter = filter;
	}

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

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		final ReturnableType<?> resultType = resolveResultType( walker );

		List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		ArgumentsValidator argumentsValidator = getArgumentsValidator();
		if ( argumentsValidator != null ) {
			argumentsValidator.validateSqlTypes( arguments, getFunctionName() );
		}
		return new SelfRenderingAggregateFunctionSqlAstExpression(
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
	public void appendHqlString(StringBuilder sb) {
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		sb.append( getFunctionName() );
		sb.append( '(' );
		int i = 1;
		if ( arguments.get( 0 ) instanceof SqmDistinct<?> ) {
			arguments.get( 0 ).appendHqlString( sb );
			if ( arguments.size() > 1 ) {
				sb.append( ' ' );
				arguments.get( 1 ).appendHqlString( sb );
				i = 2;
			}
		}
		for ( ; i < arguments.size(); i++ ) {
			sb.append(", ");
			arguments.get( i ).appendHqlString( sb );
		}

		sb.append( ')' );
		if ( filter != null ) {
			sb.append( " filter (where " );
			filter.appendHqlString( sb );
			sb.append( ')' );
		}
	}
}
