/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.Map;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server xmlelement function.
 */
public class SQLServerXmlElementFunction extends XmlElementFunction {

	public SQLServerXmlElementFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			XmlElementArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "(select 1 tag,null parent" );
		final String aliasPrefix = " [" + arguments.elementName() + "!1";
		if ( arguments.attributes() != null ) {
			for ( Map.Entry<String, Expression> entry : arguments.attributes().getAttributes().entrySet() ) {
				sqlAppender.appendSql( ',' );
				entry.getValue().accept( walker );
				sqlAppender.appendSql( aliasPrefix );
				sqlAppender.appendSql( '!' );
				sqlAppender.appendSql( entry.getKey() );
				sqlAppender.appendSql( ']' );
			}
		}
		else if ( arguments.content().isEmpty() ) {
			sqlAppender.appendSql( ",null" );
			sqlAppender.appendSql( aliasPrefix );
			sqlAppender.appendSql( ']' );
		}
		if ( !arguments.content().isEmpty() ) {
			for ( Expression expression : arguments.content() ) {
				sqlAppender.appendSql( ',' );
				expression.accept( walker );
				sqlAppender.appendSql( aliasPrefix );
				sqlAppender.appendSql( ']' );
			}
		}
		sqlAppender.appendSql( " for xml explicit,type)" );
	}
}
