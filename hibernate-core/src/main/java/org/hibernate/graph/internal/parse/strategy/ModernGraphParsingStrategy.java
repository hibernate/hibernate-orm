/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse.strategy;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.GraphLanguageLexer;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.internal.parse.GraphParser;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ModernGraphParsingStrategy implements GraphParsingStrategy {

	@Override
	public <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> entityDomainType,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}
		final var parser = new GraphLanguageParser( new CommonTokenStream( new GraphLanguageLexer(
				CharStreams.fromString( graphText ) ) ) );
		final var graph = parser.graph();

		return parse( entityDomainType, graph.graphElementList(), sessionFactory );
	}

	@Override
	public <T> RootGraphImplementor<T> parse(String graphText, SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException(
				"Parsing of graph text is not supported with 'modern' graph parser mode" );
	}

	@Override
	public void parseInto(GraphImplementor<?> graph, String graphText, SessionFactoryImplementor sessionFactory) {
		final var parser = new GraphLanguageParser( new CommonTokenStream( new GraphLanguageLexer(
				CharStreams.fromString( graphText ) ) ) );
		final var graphCtx = parser.graph();
		final var visitor = new GraphParser( sessionFactory );
		visitor.getGraphStack().push( graph );
		try {
			visitor.visitGraphElementList( graphCtx.graphElementList() );
		}
		finally {
			visitor.getGraphStack().pop();
			assert visitor.getGraphStack().isEmpty();
		}
	}

	public static <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> rootType,
			GraphLanguageParser.GraphElementListContext graphElementListContext,
			SessionFactoryImplementor sessionFactory) {
		return parse( null, rootType, graphElementListContext, sessionFactory.getJpaMetamodel()::findEntityType );
	}


	public static <T> RootGraphImplementor<T> parse(
			@Nullable String name,
			EntityDomainType<T> rootType,
			GraphLanguageParser.GraphElementListContext graphElementListContext,
			GraphParserEntityNameResolver entityNameResolver) {
		final RootGraphImpl<T> targetGraph = new RootGraphImpl<>( name, rootType );
		final var visitor = new GraphParser( entityNameResolver );
		visitor.getGraphStack().push( targetGraph );
		try {
			visitor.visitGraphElementList( graphElementListContext );
		}
		finally {
			visitor.getGraphStack().pop();
			assert visitor.getGraphStack().isEmpty();
		}
		return targetGraph;
	}
}
