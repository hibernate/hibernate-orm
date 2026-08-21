/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
 * due to https://github.com/h2database/h2database/issues/1815.
 * This emulation uses {@code array_get}, {@code array_length} and {@code system_range} functions to roughly achieve the same.
 */
public class H2ArrayToStringFunction extends ArrayToStringFunction {

	private final int maximumArraySize;

	public H2ArrayToStringFunction(int maximumArraySize, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression separatorExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression defaultExpression = sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) arrayExpression.getExpressionType().getSingleJdbcMapping();
		final int ddlTypeCode = pluralType.getElementType().getJdbcType().getDdlTypeCode();
		final boolean needsCast = !SqlTypes.isStringType( ddlTypeCode );
		if ( arrayExpression instanceof SelfRenderingOrderedSetAggregateFunctionSqlAstExpression<?> functionExpression
			&& ArrayAggFunction.FUNCTION_NAME.equals( functionExpression.getFunctionName() ) ) {
			// When the array argument is an aggregate expression, we access its contents directly
			final Expression arrayElementExpression = (Expression) functionExpression.getArguments().get( 0 );
			final List<SortSpecification> withinGroup = functionExpression.getWithinGroup();
			final Predicate filter = functionExpression.getFilter();

			sqlAppender.append( "listagg(" );
			if ( defaultExpression != null ) {
				sqlAppender.append( "coalesce(" );
			}
			if ( needsCast ) {
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					// By default, H2 uses upper case, so lower it for a consistent experience
					sqlAppender.append( "lower(" );
				}
				sqlAppender.append( "cast(" );
			}
			arrayElementExpression.accept( walker );
			if ( needsCast ) {
				sqlAppender.append( " as varchar)" );
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					sqlAppender.append( ')' );
				}
			}
			if ( defaultExpression != null ) {
				sqlAppender.append( ',' );
				defaultExpression.accept( walker );
				sqlAppender.append( ')' );
			}
			sqlAppender.append( "," );
			walker.render( separatorExpression, SqlAstNodeRenderingMode.DEFAULT );
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
			if ( filter != null ) {
				walker.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( walker );
				sqlAppender.appendSql( ')' );
				walker.getCurrentClauseStack().pop();
			}
		}
		else {
			sqlAppender.append( "case when " );
			arrayExpression.accept( walker );
			sqlAppender.append( " is not null then coalesce((select listagg(" );
			if ( defaultExpression != null ) {
				sqlAppender.append( "coalesce(" );
			}
			if ( needsCast ) {
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					// By default, H2 uses upper case, so lower it for a consistent experience
					sqlAppender.append( "lower(" );
				}
				sqlAppender.append( "cast(" );
			}
			sqlAppender.append( "array_get(" );
			arrayExpression.accept( walker );
			sqlAppender.append( ",i.idx)" );
			if ( needsCast ) {
				sqlAppender.append( " as varchar)" );
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					sqlAppender.append( ')' );
				}
			}
			if ( defaultExpression != null ) {
				sqlAppender.append( ',' );
				defaultExpression.accept( walker );
				sqlAppender.append( ')' );
			}
			sqlAppender.append( "," );
			walker.render( separatorExpression, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.append( ") within group (order by i.idx) from system_range(1," );
			sqlAppender.append( Integer.toString( maximumArraySize ) );
			sqlAppender.append( ") i(idx) where i.idx<=coalesce(cardinality(" );
			arrayExpression.accept( walker );
			sqlAppender.append( "),0)),'') end" );
		}
	}
}
