/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

/**
 * Gaussdb array_trim function.
 *
 * Notes: Original code of this class is based on PostgreSQLArrayTrimEmulation.
 */
public class GaussDBArrayTrimFunction extends AbstractArrayTrimFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression lengthExpression = (Expression) sqlAstArguments.get( 1 );

		sqlAppender.append( "array_trim(");
		arrayExpression.accept( walker );
		sqlAppender.append( ",");
		lengthExpression.accept( walker );
		sqlAppender.append( ")");

	}
}
