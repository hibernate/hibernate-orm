/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;


public class SpannerPostgreSQLRegexpLikeFunction extends AbstractRegexpLikeFunction {

	public SpannerPostgreSQLRegexpLikeFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
		sqlAppender.append( "regexp_match(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( "," );
		sqlAstArguments.get( 1 ).accept( walker );
		if (sqlAstArguments.size() > 2) {
			sqlAppender.append( "," );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.append( ") IS NOT NULL" );
	}
}
