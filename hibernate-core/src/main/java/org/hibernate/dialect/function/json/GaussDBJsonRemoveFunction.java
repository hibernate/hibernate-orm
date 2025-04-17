/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_remove function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonSetFunction.
 */
public class GaussDBJsonRemoveFunction extends AbstractJsonRemoveFunction {

	public GaussDBJsonRemoveFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {

		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		sqlAppender.appendSql( "json_remove(" );
		json.accept( translator );
		sqlAppender.appendSql( "," );
		jsonPath.accept( translator );
		sqlAppender.appendSql( ")" );
	}

	private boolean isJsonType(Expression expression) {
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		return expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson();
	}
}
