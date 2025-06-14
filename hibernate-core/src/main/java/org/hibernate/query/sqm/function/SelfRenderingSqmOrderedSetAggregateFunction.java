/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		hql.append( getFunctionName() );
		hql.append( '(' );
		if ( !arguments.isEmpty() ) {
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
				hql.append( ", " );
				arguments.get( i ).appendHqlString( hql, context );
			}
		}
		hql.append( ')' );
		if ( withinGroup != null ) {
			hql.append( " within group (order by " );
			final List<SqmSortSpecification> sortSpecifications = withinGroup.getSortSpecifications();
			if ( !sortSpecifications.isEmpty() ) {
				sortSpecifications.get( 0 ).appendHqlString( hql, context );
				for ( int j = 1; j < sortSpecifications.size(); j++ ) {
					hql.append( ", " );
					sortSpecifications.get( j ).appendHqlString( hql, context );
				}
			}
			hql.append( ')' );
		}

		if ( getFilter() != null ) {
			hql.append( " filter (where " );
			getFilter().appendHqlString( hql, context );
			hql.append( ')' );
		}
	}
}
