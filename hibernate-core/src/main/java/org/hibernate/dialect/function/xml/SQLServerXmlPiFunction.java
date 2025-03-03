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
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server xmlpi function.
 */
public class SQLServerXmlPiFunction extends XmlPiFunction {

	public SQLServerXmlPiFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "cast('<?" );
		final Literal literal = (Literal) sqlAstArguments.get( 0 );
		sqlAppender.appendSql( (String) literal.getLiteralValue() );

		if ( sqlAstArguments.size() > 1 ) {
			sqlAppender.appendSql( " '+" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( "+'?>'" );
		}
		else {
			sqlAppender.appendSql( "?>'" );
		}
		sqlAppender.appendSql( " as xml)" );
	}
}
