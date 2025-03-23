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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server xmlconcat function.
 */
public class SQLServerXmlConcatFunction extends XmlConcatFunction {

	public SQLServerXmlConcatFunction(TypeConfiguration typeConfiguration) {
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
			sqlAppender.appendSql( separator );
			sqlAppender.appendSql( "cast(" );
			sqlAstArgument.accept( walker );
			sqlAppender.appendSql( " as nvarchar(max))" );
			separator = '+';
		}
		sqlAppender.appendSql( " as xml)" );
	}
}
