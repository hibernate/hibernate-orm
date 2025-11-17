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
 * DB2 10.5 xmlexists function.
 */
public class LegacyDB2XmlExistsFunction extends XmlExistsFunction {

	public LegacyDB2XmlExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final String xquery = walker.getLiteralValue( (Expression) sqlAstArguments.get( 0 ) );
		final Expression xmlDocument = (Expression) sqlAstArguments.get( 1 );
		final boolean needsCast = !ExpressionTypeHelper.isXml( xmlDocument );
		sqlAppender.appendSql( "xmlexists(" );
		sqlAppender.appendSingleQuoteEscapedString( "$d" + xquery );
		sqlAppender.appendSql( " passing " );
		if ( needsCast ) {
			sqlAppender.appendSql( "xmlparse(document " );
		}
		sqlAstArguments.get( 1 ).accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( " as \"d\")" );
	}
}
