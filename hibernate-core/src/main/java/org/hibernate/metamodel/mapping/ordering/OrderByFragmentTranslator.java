/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.hibernate.QueryException;
import org.hibernate.grammars.ordering.OrderingLexer;
import org.hibernate.grammars.ordering.OrderingParser;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.ast.ParseTreeVisitor;


import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.hibernate.query.SyntaxException;
import org.hibernate.query.sqm.ParsingException;

import static org.hibernate.query.hql.internal.StandardHqlTranslator.prettifyAntlrError;

/**
 * Responsible for performing the translation of the order-by fragment associated
 * with an order set or map.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.OrderBy
 */
public class OrderByFragmentTranslator {

	/**
	 * Perform the translation of the user-supplied fragment, returning the translation.
	 *
	 * @return The translation.
	 *
	 * @apiNote The important distinction to this split between (1) translating and (2) resolving aliases is that
	 * both happen at different times.  This is performed at boot-time while building the CollectionPersister
	 * happens at runtime while loading the described collection
	 */
	public static OrderByFragment translate(
			String fragment,
			PluralAttributeMapping pluralAttributeMapping,
			TranslationContext context) {
		final var parseTree = buildParseTree( fragment );
		final var visitor = new ParseTreeVisitor( pluralAttributeMapping, context );
		return new OrderByFragmentImpl( visitor.visitOrderByFragment( parseTree ) );
	}

	public static void check(String fragment) {
		final var parseTree = buildParseTree( fragment );
		// TODO: check against the model (requires the PluralAttributeMapping)
	}

	private static OrderingParser.OrderByFragmentContext buildParseTree(String fragment) {
		final var lexer = new OrderingLexer( CharStreams.fromString( fragment ) );

		final var parser = new OrderingParser( new CommonTokenStream( lexer ) );

		// try to use SLL(k)-based parsing first - it's faster
		parser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		parser.removeErrorListeners();
		parser.setErrorHandler( new BailErrorStrategy() );

		try {
			return parser.orderByFragment();
		}
		catch (ParseCancellationException e) {
			// When resetting the parser, its CommonTokenStream will seek(0) i.e. restart emitting buffered tokens.
			// This is enough when reusing the lexer and parser, and it would be wrong to also reset the lexer.
			// Resetting the lexer causes it to hand out tokens again from the start, which will then append to the
			// CommonTokenStream and cause a wrong parse
			// lexer.reset();

			// reset the input token stream and parser state
			parser.reset();

			// fall back to LL(k)-based parsing
			parser.getInterpreter().setPredictionMode( PredictionMode.LL );
//			parser.addErrorListener( ConsoleErrorListener.INSTANCE );
			parser.setErrorHandler( new DefaultErrorStrategy() );

			final ANTLRErrorListener errorListener = new BaseErrorListener() {
				@Override
				public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
					throw new SyntaxException( prettifyAntlrError( offendingSymbol, line, charPositionInLine, msg, e, fragment, true ), fragment );
				}
			};
			parser.addErrorListener( errorListener );

			return parser.orderByFragment();
		}
		catch ( ParsingException ex ) {
			// Note that this is supposed to represent a bug in the parser
			throw new QueryException( "Failed to interpret syntax [" + ex.getMessage() + "]", fragment, ex );
		}
	}

}
