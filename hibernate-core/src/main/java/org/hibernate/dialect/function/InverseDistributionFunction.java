/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SelfRenderingSqmAggregateFunction;
import org.hibernate.query.sqm.function.SelfRenderingSqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class InverseDistributionFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public InverseDistributionFunction(String name, FunctionParameterType parameterType, TypeConfiguration typeConfiguration) {
		super(
				name,
				FunctionKind.ORDERED_SET_AGGREGATE,
				parameterType == null
						? StandardArgumentsValidators.exactly( 0 )
						: new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 1 ), parameterType ),
				null
		);
	}

	@Override
	public <T> SelfRenderingSqmAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
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
			protected ReturnableType<?> resolveResultType(TypeConfiguration typeConfiguration) {
				return (ReturnableType<?>) withinGroupClause.getSortSpecifications().get( 0 ).getSortExpression()
							.getExpressible();
			}

			@Override
			protected MappingModelExpressible<?> getMappingModelExpressible(
					SqmToSqlAstConverter walker,
					ReturnableType<?> resultType) {
				MappingModelExpressible<?> mapping;
				if ( resultType instanceof MappingModelExpressible) {
					// here we have a BasicType, which can be cast
					// directly to BasicValuedMapping
					mapping = (MappingModelExpressible<?>) resultType;
				}
				else {
					// here we have something that is not a BasicType,
					// and we have no way to get a BasicValuedMapping
					// from it directly
					final Expression expression = (Expression) withinGroupClause.getSortSpecifications().get( 0 )
							.getSortExpression()
							.accept( walker );
					if ( expression.getExpressionType() instanceof BasicValuedMapping ) {
						return (BasicValuedMapping) expression.getExpressionType();
					}
					try {
						final MappingMetamodelImplementor domainModel = walker.getCreationContext()
								.getSessionFactory()
								.getRuntimeMetamodels()
								.getMappingMetamodel();
						return domainModel.resolveMappingExpressible(
								getNodeType(),
								walker.getFromClauseAccess()::getTableGroup
						);
					}
					catch (Exception e) {
						return null; // this works at least approximately
					}
				}
				return mapping;
			}
		};
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, Collections.emptyList(), walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, filter, Collections.emptyList(), walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
		sqlAppender.appendSql( getName() );
		sqlAppender.appendSql( '(' );
		if ( !sqlAstArguments.isEmpty() ) {
			if ( caseWrapper ) {
				sqlAppender.appendSql( "case when " );
				filter.accept( translator );
				sqlAppender.appendSql( " then " );
				sqlAstArguments.get( 0 ).accept( translator );
				sqlAppender.appendSql( " else null end" );
			}
			else {
				sqlAstArguments.get( 0 ).accept( translator );
			}
		}
		else if ( caseWrapper ) {
			throw new IllegalArgumentException( "Can't emulate filter clause for function [" + getName() + "] without arguments!" );
		}
		sqlAppender.appendSql( ')' );
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			sqlAppender.appendSql( " within group (order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			sqlAppender.appendSql( ')' );
		}
		if ( filter != null ) {
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
		}
	}

}
