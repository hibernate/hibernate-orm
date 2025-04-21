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
 * GaussDB json_array_insert function.
 *
 * @author liubao
 * <p>
 * Notes: Original code of this class is based on PostgreSQLJsonArrayInsertFunction.
 */
public class GaussDBJsonArrayInsertFunction extends AbstractJsonArrayInsertFunction {

	public GaussDBJsonArrayInsertFunction(TypeConfiguration typeConfiguration) {
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

		sqlAppender.append( "json_array_insert(" );
		json.accept( translator );
		sqlAppender.append( "," );
		jsonPath.accept( translator );
		sqlAppender.append( "," );
		value.accept( translator );
		sqlAppender.append( ")" );
	}
}
