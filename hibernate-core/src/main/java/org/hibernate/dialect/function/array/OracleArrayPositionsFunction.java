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
import org.hibernate.type.spi.TypeConfiguration;

public class OracleArrayPositionsFunction extends AbstractArrayPositionsFunction {

	public OracleArrayPositionsFunction(boolean list, TypeConfiguration typeConfiguration) {
		super( list, typeConfiguration );
	}

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
		sqlAppender.appendSql( arrayTypeName );
		sqlAppender.append( "_positions(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ")" );
	}
}
