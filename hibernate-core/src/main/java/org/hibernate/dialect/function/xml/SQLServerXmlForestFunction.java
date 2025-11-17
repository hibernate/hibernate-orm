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
 * SQL Server xmlforest function.
 */
public class SQLServerXmlForestFunction extends XmlForestFunction {

	public SQLServerXmlForestFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "cast" );
		char separator = '(';
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			final AliasedExpression expression = (AliasedExpression) sqlAstArgument;
			sqlAppender.appendSql( separator );
			sqlAppender.appendSql( "cast((select 1 tag,null parent," );
			expression.getExpression().accept( walker );
			sqlAppender.appendSql( " [" );
			sqlAppender.appendSql( expression.getAlias() );
			sqlAppender.appendSql( "!1] for xml explicit,type) as nvarchar(max))" );
			separator = '+';
		}
		sqlAppender.appendSql( " as xml)" );
	}
}
