/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Oracle array_to_string function.
 */
public class OracleArrayToStringFunction extends ArrayToStringFunction {

	public OracleArrayToStringFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final JdbcMappingContainer expressionType = (arrayExpression).getExpressionType();
		if ( arrayExpression instanceof SelfRenderingOrderedSetAggregateFunctionSqlAstExpression
				&& ArrayAggFunction.FUNCTION_NAME.equals( ( (FunctionExpression) arrayExpression ).getFunctionName() ) ) {
			final SelfRenderingOrderedSetAggregateFunctionSqlAstExpression functionExpression
					= (SelfRenderingOrderedSetAggregateFunctionSqlAstExpression) arrayExpression;
			// When the array argument is an aggregate expression, we access its contents directly
			final Expression arrayElementExpression = (Expression) functionExpression.getArguments().get( 0 );
			final @Nullable Expression defaultExpression =
					sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
			final List<SortSpecification> withinGroup = functionExpression.getWithinGroup();
			final Predicate filter = functionExpression.getFilter();

			sqlAppender.append( "listagg(" );
			if ( filter != null ) {
				sqlAppender.appendSql( "case when " );
				walker.getCurrentClauseStack().push( Clause.WHERE );
				filter.accept( walker );
				walker.getCurrentClauseStack().pop();
				sqlAppender.appendSql( " then " );
			}
			if ( defaultExpression != null ) {
				sqlAppender.append( "coalesce(" );
			}
			arrayElementExpression.accept( walker );
			if ( defaultExpression != null ) {
				sqlAppender.append( ',' );
				defaultExpression.accept( walker );
				sqlAppender.append( ')' );
			}
			if ( filter != null ) {
				sqlAppender.appendSql( " else null end" );
			}
			sqlAppender.append( ',' );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ')' );

			if ( withinGroup != null && !withinGroup.isEmpty() ) {
				walker.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
				sqlAppender.appendSql( " within group (order by " );
				withinGroup.get( 0 ).accept( walker );
				for ( int i = 1; i < withinGroup.size(); i++ ) {
					sqlAppender.appendSql( ',' );
					withinGroup.get( i ).accept( walker );
				}
				sqlAppender.appendSql( ')' );
				walker.getCurrentClauseStack().pop();
			}
		}
		else if ( expressionType.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ==  SqlTypes.JSON_ARRAY ) {
			sqlAppender.append( "(select listagg(" );
			if ( sqlAstArguments.size() > 2 ) {
				sqlAppender.append( "coalesce(t.v," );
				sqlAstArguments.get( 2 ).accept( walker );
				sqlAppender.append( ")," );
			}
			else {
				sqlAppender.append( "t.v," );
			}

			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ") from json_table(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.append( ",'$[*]' columns (v path '$')) t)" );
		}
		else {
			final String arrayTypeName = DdlTypeHelper.getTypeName(
					expressionType,
					walker.getSessionFactory().getTypeConfiguration()
			);
			sqlAppender.append( arrayTypeName );
			sqlAppender.append( "_to_string(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.append( ',' );
			sqlAstArguments.get( 1 ).accept( walker );
			if ( sqlAstArguments.size() > 2 ) {
				sqlAppender.append( ',' );
				sqlAstArguments.get( 2 ).accept( walker );
			}
			else {
				sqlAppender.append( ",null" );
			}
			sqlAppender.append( ')' );
		}
	}
}
