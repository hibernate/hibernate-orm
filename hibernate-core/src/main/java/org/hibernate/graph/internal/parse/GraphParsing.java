/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.GraphParserMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.GraphLanguageLexer;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.grammars.graph.LegacyGraphLanguageParser;
import org.hibernate.graph.internal.parse.strategy.ModernGraphParsingStrategy;
import org.hibernate.graph.internal.parse.strategy.GraphParsingStrategy;
import org.hibernate.graph.internal.parse.strategy.LegacyGraphParsingStrategy;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * Helper for dealing with graph text parsing.
 *
 * @author Steve Ebersole
 */
public class GraphParsing {

	public static <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> entityDomainType,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		return getGraphParsingStrategy( sessionFactory ).parse(
				entityDomainType,
				graphText,
				sessionFactory
		);
	}

	public static <T> RootGraphImplementor<T> parse(
			Class<T> entityClass,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		return parse( sessionFactory.getJpaMetamodel().entity( entityClass ),
				graphText, sessionFactory );
	}

	public static RootGraphImplementor<?> parse(
			String entityName,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		return parse( sessionFactory.getJpaMetamodel().entity( entityName ),
				graphText, sessionFactory );
	}

	@Deprecated(forRemoval = true)
	public static RootGraphImplementor<?> parse(
			String graphText,
			SessionFactoryImplementor sessionFactory) {

		return getGraphParsingStrategy( sessionFactory ).parse( graphText, sessionFactory );
	}

	public static <T> RootGraphImplementor<T> visit(
			EntityDomainType<T> rootType,
			LegacyGraphLanguageParser.AttributeListContext attributeListContext,
			SessionFactoryImplementor sessionFactory) {
		return visit( rootType, attributeListContext, sessionFactory.getJpaMetamodel()::findEntityType );
	}

	public static <T> RootGraphImplementor<T> visit(
			EntityDomainType<T> rootType,
			LegacyGraphLanguageParser.AttributeListContext attributeListContext,
			GraphParserEntityNameResolver entityNameResolver) {
		return visit( null, rootType, attributeListContext, entityNameResolver );
	}

	@Deprecated(forRemoval = true)
	public static LegacyGraphLanguageParser.@NonNull GraphContext parseLegacyGraphText(String graphText) {
		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new LegacyGraphLanguageParser( new CommonTokenStream( lexer ) );
		return parser.graph();
	}

	public static GraphLanguageParser.@NonNull GraphContext parseText(String graphText) {
		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		return parser.graph();
	}

	public static <T> RootGraphImplementor<T> visit(
			@Nullable String name,
			EntityDomainType<T> rootType,
			LegacyGraphLanguageParser.AttributeListContext graphElementListContext,
			GraphParserEntityNameResolver entityNameResolver) {

		return LegacyGraphParsingStrategy.parse( name, rootType, graphElementListContext, entityNameResolver );
	}

	public static <T> RootGraphImplementor<T> visit(
			@Nullable String name,
			EntityDomainType<T> rootType,
			GraphLanguageParser.GraphElementListContext graphElementListContext,
			GraphParserEntityNameResolver entityNameResolver) {

		return ModernGraphParsingStrategy.parse( name, rootType, graphElementListContext, entityNameResolver );
	}

	/**
	 * Parse the passed graph textual representation into the passed Graph.
	 * Essentially overlays the text representation on top of the graph.
	 */
	public static void parseInto(
			GraphImplementor<?> targetGraph,
			CharSequence graphString,
			SessionFactoryImplementor sessionFactory) {

		getGraphParsingStrategy( sessionFactory ).parseInto( targetGraph, graphString.toString(), sessionFactory );
	}

	private static GraphParsingStrategy getGraphParsingStrategy(SessionFactoryImplementor sessionFactory) {
		final GraphParserMode mode = sessionFactory.getSessionFactoryOptions().getGraphParserMode();

		if ( mode == GraphParserMode.MODERN ) {
			return new ModernGraphParsingStrategy();
		}

		return new LegacyGraphParsingStrategy();
	}

}
