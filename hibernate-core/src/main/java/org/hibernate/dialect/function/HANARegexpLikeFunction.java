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


/**
 * HANA has a special predicate.
 */
public class HANARegexpLikeFunction extends AbstractRegexpLikeFunction {

	public HANARegexpLikeFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		arguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( " like_regexpr " );
		arguments.get( 1 ).accept( walker );
		if ( arguments.size() > 2 ) {
			sqlAppender.appendSql( " flag " );
			arguments.get( 2 ).accept( walker );
		}
	}

}
