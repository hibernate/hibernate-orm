/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.array.AbstractArrayPositionFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB variant of the function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLArrayPositionFunction.
 */
public class GaussDBArrayPositionFunction extends AbstractArrayPositionFunction {

	public GaussDBArrayPositionFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 1 );

		sqlAppender.append( "(array_positions(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ", " );
		walker.render( elementExpression, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.append( "))[1]" );
	}
}
