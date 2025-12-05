/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function;

import org.hibernate.dialect.function.AbstractRegexpLikeFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;


/**
 * Informix has special integer constants as third argument.
 */
public class InformixRegexpLikeFunction extends AbstractRegexpLikeFunction {

	public InformixRegexpLikeFunction(TypeConfiguration typeConfiguration) {
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
				throw new IllegalArgumentException( "Informix only supports the case insensitive flag 'i' as literal but got." );
			}
			caseSensitive = false;
		}
		else {
			caseSensitive = true;
		}

		sqlAppender.appendSql( "regex_match(" );
		arguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ',' );
		arguments.get( 1 ).accept( walker );
		if ( !caseSensitive ) {
			// 1 is extended POSIX regex which is the default, 3 is extended POSIX regex and case-insensitive
			// See https://www.ibm.com/docs/en/informix-servers/14.10.0?topic=routines-regex-match-function
			sqlAppender.appendSql( ",3" );
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	public boolean isPredicate() {
		return false;
	}
}
