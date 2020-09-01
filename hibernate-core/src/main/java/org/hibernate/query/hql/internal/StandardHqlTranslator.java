/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.QueryException;
import org.hibernate.grammars.hql.HqlLexer;
import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.sqm.internal.SqmTreePrinter;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.sqm.tree.SqmStatement;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Standard implementation of SemanticQueryInterpreter
 *
 * @author Steve Ebersole
 */
public class StandardHqlTranslator implements HqlTranslator {
	private final SqmCreationContext sqmCreationContext;
	private final SqmCreationOptions sqmCreationOptions;


	public StandardHqlTranslator(
			SqmCreationContext sqmCreationContext,
			SqmCreationOptions sqmCreationOptions) {
		this.sqmCreationContext = sqmCreationContext;
		this.sqmCreationOptions = sqmCreationOptions;
	}

	@Override
	public SqmStatement translate(String query) {
		HqlLogging.QUERY_LOGGER.debugf( "HQL : " + query );

		final HqlParser.StatementContext hqlParseTree = parseHql( query );

		// then we perform semantic analysis and build the semantic representation...
		try {
			final SqmStatement sqmStatement = SemanticQueryBuilder.buildSemanticModel(
					hqlParseTree,
					sqmCreationOptions,
					sqmCreationContext
			);

			// Log the SQM tree (if enabled)
			SqmTreePrinter.logTree( sqmStatement );

			return sqmStatement;
		}
		catch (QueryException e) {
			throw e;
		}
		catch (Exception e) {
			throw new InterpretationException( query, e );
		}
	}

	private HqlParser.StatementContext parseHql(String hql) {
		// Build the lexer
		final HqlLexer hqlLexer = HqlParseTreeBuilder.INSTANCE.buildHqlLexer( hql );

		// Build the parse tree
		final HqlParser hqlParser = HqlParseTreeBuilder.INSTANCE.buildHqlParser( hql, hqlLexer );

		// try to use SLL(k)-based parsing first - its faster
		hqlParser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		hqlParser.removeErrorListeners();
		hqlParser.setErrorHandler( new BailErrorStrategy() );

		try {
			return hqlParser.statement();
		}
		catch ( ParseCancellationException e) {
			// reset the input token stream and parser state
			hqlLexer.reset();
			hqlParser.reset();

			// fall back to LL(k)-based parsing
			hqlParser.getInterpreter().setPredictionMode( PredictionMode.LL );
			hqlParser.addErrorListener( ConsoleErrorListener.INSTANCE );
			hqlParser.setErrorHandler( new DefaultErrorStrategy() );

			return hqlParser.statement();
		}

	}
}
