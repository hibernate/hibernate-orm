/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.array.ArrayConcatFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * GaussDB variant of the function to properly return {@code null} when one of the arguments is null.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLArrayConcatFunction.
 */
public class GaussDBArrayConcatFunction extends ArrayConcatFunction {

	public GaussDBArrayConcatFunction() {
		super( "", "||", "" );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "case when " );
		String separator = "";
		for ( SqlAstNode node : sqlAstArguments ) {
			sqlAppender.append( separator );
			node.accept( walker );
			sqlAppender.append( " is not null" );
			separator = " and ";
		}

		sqlAppender.append( " then " );
		super.render( sqlAppender, sqlAstArguments, returnType, walker );
		sqlAppender.append( " end" );
	}
}
