/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_replace function.
 *
 * @author liubao
 * <p>
 * Notes: Original code of this class is based on PostgreSQLJsonSetFunction.
 */
public class GaussDBJsonReplaceFunction extends AbstractJsonReplaceFunction {

	public GaussDBJsonReplaceFunction(TypeConfiguration typeConfiguration) {
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
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "json_replace(" );
		json.accept( translator );
		sqlAppender.appendSql( "," );
		jsonPath.accept( translator );
		sqlAppender.appendSql( "," );
		value.accept( translator );
		sqlAppender.appendSql( ")" );
	}
}
