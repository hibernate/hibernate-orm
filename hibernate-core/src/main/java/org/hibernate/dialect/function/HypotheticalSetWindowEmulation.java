/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
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
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class HypotheticalSetWindowEmulation extends HypotheticalSetFunction {

	public HypotheticalSetWindowEmulation(String name, BasicTypeReference<?> returnType, TypeConfiguration typeConfiguration) {
		super(
				name,
				returnType,
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

				List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
				ArgumentsValidator argumentsValidator = getArgumentsValidator();
				if ( argumentsValidator != null ) {
					argumentsValidator.validateSqlTypes( arguments, getFunctionName() );
				}
				List<SortSpecification> withinGroup;
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
				final SelfRenderingFunctionSqlAstExpression function = new SelfRenderingOrderedSetAggregateFunctionSqlAstExpression(
						getFunctionName(),
						getFunctionRenderer(),
						Collections.emptyList(),
						getFilter() == null ? null : (Predicate) getFilter().accept( walker ),
						Collections.emptyList(),
						resultType,
						getMappingModelExpressible( walker, resultType, arguments )
				);
				final Over<Object> windowFunction = new Over<>( function, new ArrayList<>(), withinGroup );
				walker.registerQueryTransformer(
						new AggregateWindowEmulationQueryTransformer( windowFunction, withinGroup, arguments )
				);
				return windowFunction;
			}
		};
	}
}
