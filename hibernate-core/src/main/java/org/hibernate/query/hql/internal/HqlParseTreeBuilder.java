/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;

/**
 * Leverages ANTLR to build a parse tree from an HQL query.
 *
 * @author Steve Ebersole
 */
public class HqlParseTreeBuilder {
	/**
	 * Singleton access
	 */
	public static final HqlParseTreeBuilder INSTANCE = new HqlParseTreeBuilder();

	public HqlLexer buildHqlLexer(String hql) {
		return new HqlLexer( CharStreams.fromString( hql ) );
	}

	public HqlParser buildHqlParser(String hql, HqlLexer hqlLexer) {
		// Build the parser
		return new HqlParser( new CommonTokenStream( hqlLexer ) );
	}
}
