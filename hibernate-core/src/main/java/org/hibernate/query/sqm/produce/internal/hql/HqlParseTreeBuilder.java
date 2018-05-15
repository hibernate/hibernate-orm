/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql;

import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlLexer;
import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParser;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

/**
 * @author Steve Ebersole
 */
public class HqlParseTreeBuilder {
	private static final Logger log = Logger.getLogger( HqlParseTreeBuilder.class );

	/**
	 * Singleton access
	 */
	public static final HqlParseTreeBuilder INSTANCE = new HqlParseTreeBuilder();

	public HqlParser parseHql(String hql) {
		// Build the lexer
		HqlLexer hqlLexer = new HqlLexer( CharStreams.fromString( hql ) );

		// Build the parser...
		return new HqlParser( new CommonTokenStream( hqlLexer ) ) {
			@Override
			protected void logUseOfReservedWordAsIdentifier(Token token) {
				log.debugf( "Encountered use of reserved word as identifier : " + token.getText() );
			}
		};
	}
}
