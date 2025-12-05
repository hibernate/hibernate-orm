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
 * Oracle array_sort function.
 */
public class OracleArraySortFunction extends AbstractArraySortFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				arrayExpression.getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);

		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_sort(" );
		arrayExpression.accept( walker );

		if ( sqlAstArguments.size() > 1 ) {
			sqlAppender.append( ',' );
			final SqlAstNode descNode = sqlAstArguments.get( 1 );
			if ( descNode instanceof Literal literal && literal.getLiteralValue() instanceof Boolean boolValue ) {
				sqlAppender.append( boolValue ? '1' : '0' );
			}
			else {
				descNode.accept( walker );
			}

			if ( sqlAstArguments.size() > 2 ) {
				sqlAppender.append( ',' );
				final SqlAstNode nullsNode = sqlAstArguments.get( 2 );
				if ( nullsNode instanceof Literal literal && literal.getLiteralValue() instanceof Boolean boolValue ) {
					sqlAppender.append( boolValue ? '1' : '0' );
				}
				else {
					nullsNode.accept( walker );
				}
			}
		}

		sqlAppender.append( ')' );
	}
}
