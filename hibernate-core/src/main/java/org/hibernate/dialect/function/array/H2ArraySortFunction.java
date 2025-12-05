/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
 * due to https://github.com/h2database/h2database/issues/1815.
 * This emulation uses {@code array_get}, {@code cardinality} and {@code system_range}
 * functions to achieve array sorting.
 */
public class H2ArraySortFunction extends AbstractArraySortFunction {

	private final int maximumArraySize;

	public H2ArraySortFunction(int maximumArraySize, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final boolean areArgumentsLiterals =
				( sqlAstArguments.size() <= 1 || sqlAstArguments.get( 1 ) instanceof Literal ) &&
						( sqlAstArguments.size() <= 2 || sqlAstArguments.get( 2 ) instanceof Literal );

		if ( areArgumentsLiterals ) {
			renderWithLiteralArguments( sqlAppender, sqlAstArguments, walker );
		}
		else {
			renderWithExpressionArguments( sqlAppender, sqlAstArguments, walker );
		}
	}

	private void renderWithLiteralArguments(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {

		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );

		final boolean descending = sqlAstArguments.size() > 1
				&& sqlAstArguments.get( 1 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean boolValue
				? boolValue : false;

		final Boolean nullsFirst = sqlAstArguments.size() > 2
				&& sqlAstArguments.get( 2 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean boolValue
				? boolValue : null;

		final boolean actualNullsFirst = nullsFirst != null ? nullsFirst : descending;

		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",i.idx) order by array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",i.idx)" );

		sqlAppender.append( descending
									? ( actualNullsFirst ? " desc nulls first" : " desc nulls last" )
									: ( actualNullsFirst ? " asc nulls first" : " asc nulls last" ) );

		sqlAppender.append( ") from system_range(1," );
		sqlAppender.append( Integer.toString( maximumArraySize ) );
		sqlAppender.append( ") i(idx) where i.idx<=coalesce(cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( "),0)),array[]) end" );
	}

	private void renderWithExpressionArguments(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {

		assert sqlAstArguments.size() >= 2;

		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final SqlAstNode descendingNode = sqlAstArguments.get( 1 );
		final SqlAstNode nullsFirstNode = sqlAstArguments.size() > 2
				? sqlAstArguments.get( 2 )
				: descendingNode;

		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",i.idx) order by " );

		sqlAppender.append( '(' );
		nullsFirstNode.accept( walker );
		sqlAppender.append( "=(array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",i.idx) is null)) desc," );

		sqlAppender.append( "case when " );
		descendingNode.accept( walker );
		sqlAppender.append( " then array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",i.idx) end desc," );

		sqlAppender.append( "case when not " );
		descendingNode.accept( walker );
		sqlAppender.append( " then array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",i.idx) end" );

		sqlAppender.append( ") from system_range(1," );
		sqlAppender.append( Integer.toString( maximumArraySize ) );
		sqlAppender.append( ") i(idx) where i.idx<=coalesce(cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( "),0)),array[]) end" );
	}
}
