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
 * H2 xmlconcat function.
 */
public class H2XmlConcatFunction extends XmlConcatFunction {

	public H2XmlConcatFunction(TypeConfiguration typeConfiguration) {
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
			sqlAppender.appendSql( separator );
			sqlAstArgument.accept( walker );
			separator = "||";
		}
		sqlAppender.appendSql( ')' );
	}
}
