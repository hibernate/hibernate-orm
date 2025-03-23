/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.dialect.function.json.ExpressionTypeHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL xmlquery function.
 */
public class PostgreSQLXmlQueryFunction extends XmlQueryFunction {

	public PostgreSQLXmlQueryFunction(TypeConfiguration typeConfiguration) {
		super( false, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression xmlDocument = (Expression) sqlAstArguments.get( 1 );
		final boolean needsCast = !ExpressionTypeHelper.isXml( xmlDocument );
		sqlAppender.appendSql( "(select xmlagg(v) from unnest(xpath(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ',' );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		sqlAstArguments.get( 1 ).accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as xml)" );
		}
		sqlAppender.appendSql( ")) t(v))" );
	}
}
