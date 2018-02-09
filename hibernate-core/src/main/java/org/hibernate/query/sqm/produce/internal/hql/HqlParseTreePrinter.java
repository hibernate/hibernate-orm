/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql;

import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParser;
import org.hibernate.query.sqm.produce.internal.hql.grammar.HqlParserBaseListener;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class HqlParseTreePrinter extends HqlParserBaseListener {
	private static final Logger HQL_LOGGER = Logger.getLogger( "org.hibernate.sqm.hql.parseTree" );
	private static final boolean LOG_DEBUG_ENABLED = HQL_LOGGER.isDebugEnabled();

	public static void logStatementParseTree(HqlParser parser) {
		if ( !LOG_DEBUG_ENABLED ) {
			return;
		}

		ParseTreeWalker.DEFAULT.walk( new HqlParseTreePrinter( parser ), parser.statement() );
		parser.reset();
	}

	public static void logOrderByParseTree(HqlParser parser) {
		if ( !LOG_DEBUG_ENABLED ) {
			return;
		}

		ParseTreeWalker.DEFAULT.walk( new HqlParseTreePrinter( parser ), parser.orderByClause() );
		parser.reset();
	}

	private final HqlParser parser;

	private int depth = 0;

	public HqlParseTreePrinter(HqlParser parser) {
		this.parser = parser;
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		final String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];

		if ( !ruleName.endsWith( "Keyword" ) ) {
			HQL_LOGGER.debugf(
					"%s %s (%s) [`%s`]",
					enterRulePadding(),
					ctx.getClass().getSimpleName(),
					ruleName,
					ctx.getText()
			);
		}
		super.enterEveryRule( ctx );
	}

	private String enterRulePadding() {
		return pad( depth++ ) + "->";
	}

	private String pad(int depth) {
		StringBuilder buf = new StringBuilder( 2 * depth );
		for ( int i = 0; i < depth; i++ ) {
			buf.append( "  " );
		}
		return buf.toString();
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		super.exitEveryRule( ctx );

		final String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];

		if ( !ruleName.endsWith( "Keyword" ) ) {
			HQL_LOGGER.debugf(
					"%s %s (%s) [`%s`]",
					exitRulePadding(),
					ctx.getClass().getSimpleName(),
					parser.getRuleNames()[ctx.getRuleIndex()],
					ctx.getText()
			);
		}
	}

	private String exitRulePadding() {
		return pad( --depth ) + "<-";
	}
}
