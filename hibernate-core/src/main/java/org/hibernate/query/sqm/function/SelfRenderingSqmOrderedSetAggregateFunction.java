/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * @author Christian Beikov
 */
public class SelfRenderingSqmOrderedSetAggregateFunction<T> extends SelfRenderingSqmAggregateFunction<T>
		implements SqmOrderedSetAggregateFunction<T> {

	private final SqmOrderByClause withinGroup;

	/**
	 * @deprecated Use {@link #SelfRenderingSqmOrderedSetAggregateFunction(SqmFunctionDescriptor, FunctionRenderer, List, SqmPredicate, SqmOrderByClause, ReturnableType, ArgumentsValidator, FunctionReturnTypeResolver, NodeBuilder, String)} instead
	 */
	@Deprecated(forRemoval = true)
	public SelfRenderingSqmOrderedSetAggregateFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super(
				descriptor,
				renderingSupport,
				arguments,
				filter,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
		this.withinGroup = withinGroupClause;
	}

	public SelfRenderingSqmOrderedSetAggregateFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super(
				descriptor,
				renderer,
				arguments,
				filter,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
		this.withinGroup = withinGroupClause;
	}

	@Override
	public SelfRenderingSqmOrderedSetAggregateFunction<T> copy(SqmCopyContext context) {
		final SelfRenderingSqmOrderedSetAggregateFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		final SelfRenderingSqmOrderedSetAggregateFunction<T> expression = context.registerCopy(
				this,
				new SelfRenderingSqmOrderedSetAggregateFunction<>(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getFilter() == null ? null : getFilter().copy( context ),
						withinGroup == null ? null : withinGroup.copy( context ),
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
		List<SortSpecification> withinGroup;
		if ( this.withinGroup == null ) {
			withinGroup = Collections.emptyList();
		}
		else {
			walker.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			try {
				final List<SqmSortSpecification> sortSpecifications = this.withinGroup.getSortSpecifications();
				withinGroup = new ArrayList<>( sortSpecifications.size() );
				for ( SqmSortSpecification sortSpecification : sortSpecifications ) {
					final SortSpecification specification = (SortSpecification) walker.visitSortSpecification( sortSpecification );
					if ( specification != null ) {
						withinGroup.add( specification );
					}
				}
			}
			finally {
				walker.getCurrentClauseStack().pop();
			}
		}
		return new SelfRenderingOrderedSetAggregateFunctionSqlAstExpression(
				getFunctionName(),
				getFunctionRenderer(),
				arguments,
				getFilter() == null ? null : walker.visitNestedTopLevelPredicate( getFilter() ),
				withinGroup,
				resultType,
				getMappingModelExpressible( walker, resultType, arguments )
		);
	}

	@Override
	public SqmOrderByClause getWithinGroup() {
		return withinGroup;
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
		if ( withinGroup != null ) {
			sb.append( " within group (order by " );
			final List<SqmSortSpecification> sortSpecifications = withinGroup.getSortSpecifications();
			sortSpecifications.get( 0 ).appendHqlString( sb );
			for ( int j = 1; j < sortSpecifications.size(); j++ ) {
				sb.append( ", " );
				sortSpecifications.get( j ).appendHqlString( sb );
			}
			sb.append( ')' );
		}

		if ( getFilter() != null ) {
			sb.append( " filter (where " );
			getFilter().appendHqlString( sb );
			sb.append( ')' );
		}
	}
}
