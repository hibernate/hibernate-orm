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

/**
 * HSQLDB sort_array function.
 */
public class HSQLArraySortFunction extends AbstractArraySortFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );

		sqlAppender.append( "sort_array(" );
		arrayExpression.accept( walker );

		final boolean descending = sqlAstArguments.size() > 1
				&& sqlAstArguments.get( 1 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean boolValue
				? boolValue
				: false;

		final Boolean nullsFirst = sqlAstArguments.size() > 2
				&& sqlAstArguments.get( 2 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean boolValue
				? boolValue
				: null;

		final boolean actualNullsFirst = nullsFirst != null ? nullsFirst : descending;

		sqlAppender.append( descending
									? ( actualNullsFirst ? " desc nulls first" : " desc nulls last" )
									: ( actualNullsFirst ? " asc nulls first" : " asc nulls last" ) );
		sqlAppender.append( ')' );
	}
}
