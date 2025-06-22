/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering;

import org.hibernate.grammars.ordering.OrderingLexer;
import org.hibernate.grammars.ordering.OrderingParser;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.ast.ParseTreeVisitor;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Responsible for performing the translation of the order-by fragment associated
 * with an order set or map.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.OrderBy
 */
public class OrderByFragmentTranslator {
	private static final Logger LOG = Logger.getLogger( OrderByFragmentTranslator.class.getName() );

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
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Beginning parsing of order-by fragment [%s] : %s",
					pluralAttributeMapping.getCollectionDescriptor().getRole(),
					fragment
			);
		}

		final OrderingParser.OrderByFragmentContext parseTree = buildParseTree( context, fragment );

		final ParseTreeVisitor visitor = new ParseTreeVisitor( pluralAttributeMapping, context );

		return new OrderByFragmentImpl( visitor.visitOrderByFragment( parseTree ) );
	}


	private static OrderingParser.OrderByFragmentContext buildParseTree(TranslationContext context, String fragment) {
		final OrderingLexer lexer = new OrderingLexer( CharStreams.fromString( fragment ) );

		final OrderingParser parser = new OrderingParser( new BufferedTokenStream( lexer ) );

		// try to use SLL(k)-based parsing first - its faster
		parser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		parser.removeErrorListeners();
		parser.setErrorHandler( new BailErrorStrategy() );

		try {
			return parser.orderByFragment();
		}
		catch (ParseCancellationException e) {
			// reset the input token stream and parser state
			lexer.reset();
			parser.reset();

			// fall back to LL(k)-based parsing
			parser.getInterpreter().setPredictionMode( PredictionMode.LL );
			parser.addErrorListener( ConsoleErrorListener.INSTANCE );
			parser.setErrorHandler( new DefaultErrorStrategy() );

			return parser.orderByFragment();
		}
	}

}
