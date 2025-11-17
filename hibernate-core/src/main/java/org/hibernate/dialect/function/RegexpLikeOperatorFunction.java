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
 * PostgreSQL and CockroachDB have a special predicate.
 */
public class RegexpLikeOperatorFunction extends RegexpLikePredicateFunction {

	private final boolean supportsStandard;

	public RegexpLikeOperatorFunction(TypeConfiguration typeConfiguration, boolean supportsStandard) {
		super( typeConfiguration );
		this.supportsStandard = supportsStandard;
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
				if ( supportsStandard ) {
					super.render( sqlAppender, arguments, returnType, walker );
					return;
				}
				else {
					throw new IllegalArgumentException(
							"PostgreSQL and CockroachDB only support the case insensitive flag 'i' as literal." );
				}
			}
			caseSensitive = false;
		}
		else {
			caseSensitive = true;
		}

		sqlAppender.appendSql( '(' );
		arguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( caseSensitive ? "~" : "~*" );
		arguments.get( 1 ).accept( walker );
		sqlAppender.appendSql( ')' );
	}

	@Override
	public boolean isPredicate() {
		return false;
	}
}
