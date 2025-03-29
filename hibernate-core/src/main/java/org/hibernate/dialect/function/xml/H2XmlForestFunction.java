/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 xmlforest function.
 */
public class H2XmlForestFunction extends XmlForestFunction {

	public H2XmlForestFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		String separator = "";
		sqlAppender.appendSql( '(' );
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			final AliasedExpression expression = (AliasedExpression) sqlAstArgument;
			sqlAppender.appendSql( separator );
			sqlAppender.appendSql( "xmlnode(" );
			sqlAppender.appendSingleQuoteEscapedString( expression.getAlias() );
			sqlAppender.appendSql( ",null," );
			expression.getExpression().accept( walker );
			sqlAppender.appendSql( ",false)" );
			separator = "||";
		}
		sqlAppender.appendSql( ')' );
	}
}
