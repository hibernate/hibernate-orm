/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class InverseDistributionWindowEmulation extends InverseDistributionFunction {

	public InverseDistributionWindowEmulation(String name, FunctionParameterType parameterType, TypeConfiguration typeConfiguration) {
		super(
				name,
				parameterType,
				typeConfiguration
		);
	}

	@Override
	public <T> SelfRenderingSqmOrderedSetAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new SelfRenderingInverseDistributionFunction<>(
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				queryEngine
		) {

			@Override
			public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
				final Clause currentClause = walker.getCurrentClauseStack().getCurrent();
				if ( currentClause == Clause.OVER ) {
					return super.convertToSqlAst( walker );
				}
				else if ( currentClause != Clause.SELECT ) {
					throw new IllegalArgumentException( "Can't emulate [" + getName() + "] in clause " + currentClause + ". Only the SELECT clause is supported" );
				}
				final ReturnableType<?> resultType = resolveResultType( walker );

				final List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
				final ArgumentsValidator argumentsValidator = getArgumentsValidator();
				if ( argumentsValidator != null ) {
					argumentsValidator.validateSqlTypes( arguments, getFunctionName() );
				}
				final List<SortSpecification> withinGroup;
				if ( this.getWithinGroup() == null ) {
					withinGroup = Collections.emptyList();
				}
				else {
					walker.getCurrentClauseStack().push( Clause.ORDER );
					try {
						final List<SqmSortSpecification> sortSpecifications = this.getWithinGroup().getSortSpecifications();
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
				final SelfRenderingFunctionSqlAstExpression<?> function =
						new SelfRenderingOrderedSetAggregateFunctionSqlAstExpression<>(
								getFunctionName(),
								getFunctionRenderer(),
								arguments,
								getFilter() == null ? null : (Predicate) getFilter().accept( walker ),
								withinGroup,
								resultType,
								getMappingModelExpressible( walker, resultType, arguments )
						);
				final Over<Object> windowFunction = new Over<>( function, new ArrayList<>(), Collections.emptyList() );
				walker.registerQueryTransformer(
						new AggregateWindowEmulationQueryTransformer( windowFunction, withinGroup, null )
				);
				return windowFunction;
			}
		};
	}

}
