/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;


/**
 * HSQLDB doesn't support the third argument.
 */
public class HSQLRegexpLikeFunction extends AbstractRegexpLikeFunction {

	public HSQLRegexpLikeFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final boolean caseSensitive;
		if ( arguments.size() > 2 ) {
			if ( !(arguments.get( 2 ) instanceof Literal literal)
				|| !(literal.getLiteralValue() instanceof String flags)
				|| !flags.equals( "i" ) ) {
				throw new IllegalArgumentException( "HSQLDB only supports the case insensitive flag 'i' as literal." );
			}
			caseSensitive = false;
		}
		else {
			caseSensitive = true;
		}

		sqlAppender.appendSql( "regexp_like(" );
		if ( !caseSensitive ) {
			sqlAppender.appendSql( "lower(" );
		}
		arguments.get( 0 ).accept( walker );
		if ( !caseSensitive ) {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( ',' );
		if ( !caseSensitive ) {
			sqlAppender.appendSql( "lower(" );
		}
		arguments.get( 1 ).accept( walker );
		if ( !caseSensitive ) {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	public boolean isPredicate() {
		return false;
	}
}
