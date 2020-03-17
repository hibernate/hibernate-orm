/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering;

import java.util.List;

import org.hibernate.grammars.ordering.OrderingLexer;
import org.hibernate.grammars.ordering.OrderingParser;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.ast.OrderingSpecification;
import org.hibernate.metamodel.mapping.ordering.ast.ParseTreeVisitor;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

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
 * @see javax.persistence.OrderBy
 * @see org.hibernate.annotations.OrderBy
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentTranslator {
	private static final Logger LOG = Logger.getLogger( OrderByFragmentTranslator.class.getName() );

	/**
	 * Perform the translation of the user-supplied fragment, returning the translation.
	 *
	 * @apiNote The important distinction to this split between (1) translating and (2) resolving aliases is that
	 * both happen at different times.  This is performed at boot-time while building the CollectionPersister
	 * happens at runtime while loading the described collection
	 *
	 * @return The translation.
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

		final List<OrderingSpecification> specs = visitor.visitOrderByFragment( parseTree );

		return new OrderByFragment() {
			private final List<OrderingSpecification> fragmentSpecs = specs;

			@Override
			public void apply(QuerySpec ast, TableGroup tableGroup, SqlAstCreationState creationState) {
				for ( int i = 0; i < fragmentSpecs.size(); i++ ) {
					final OrderingSpecification orderingSpec = fragmentSpecs.get( i );

					orderingSpec.getExpression().apply(
							ast,
							tableGroup,
							orderingSpec.getCollation(),
							orderingSpec.getSortOrder(),
							creationState
					);
				}
			}
		};
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
		catch ( ParseCancellationException e) {
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
