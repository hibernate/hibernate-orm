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
 * H2 xmlpi function.
 */
public class H2XmlPiFunction extends XmlPiFunction {

	public H2XmlPiFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "'<?" );
		final Literal literal = (Literal) sqlAstArguments.get( 0 );
		sqlAppender.appendSql( (String) literal.getLiteralValue() );

		if ( sqlAstArguments.size() > 1 ) {
			sqlAppender.appendSql( " '||" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( "||'?>'" );
		}
		else {
			sqlAppender.appendSql( "?>'" );
		}
	}
}
