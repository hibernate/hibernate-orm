/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse.strategy;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.ModernGraphLanguageLexer;
import org.hibernate.grammars.graph.ModernGraphLanguageParser;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.internal.parse.EntityNameResolver;
import org.hibernate.graph.internal.parse.EntityNameResolverSessionFactory;
import org.hibernate.graph.internal.parse.ModernGraphParser;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ModernGraphParsingStrategy implements GraphParsingStrategy {

	@Override
	public <T> RootGraphImplementor<T> parse(
			Class<T> entityClass,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}
		final var parser = new ModernGraphLanguageParser( new CommonTokenStream( new ModernGraphLanguageLexer(
				CharStreams.fromString( graphText ) ) ) );
		final var graph = parser.graph();


		final EntityDomainType<T> entityType = sessionFactory.getJpaMetamodel().entity( entityClass );
		return parse( entityType, graph.graphElementList(), sessionFactory );
	}

	@Override
	public <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> entityDomainType,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}
		final var parser = new ModernGraphLanguageParser( new CommonTokenStream( new ModernGraphLanguageLexer(
				CharStreams.fromString( graphText ) ) ) );
		final var graph = parser.graph();

		return parse( entityDomainType, graph.graphElementList(), sessionFactory );
	}

	@Override
	public <T> RootGraphImplementor<T> parse(
			String entityName,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}
		final var parser = new ModernGraphLanguageParser( new CommonTokenStream( new ModernGraphLanguageLexer(
				CharStreams.fromString( graphText ) ) ) );
		final var graph = parser.graph();

		@SuppressWarnings(
				"unchecked") final EntityDomainType<T> entityType = (EntityDomainType<T>) sessionFactory.getJpaMetamodel()
				.entity( entityName );
		return parse( entityType, graph.graphElementList(), sessionFactory );
	}

	@Override
	public <T> RootGraphImplementor<T> parse(String graphText, SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException(
				"Parsing of graph text is not supported with 'modern' graph parser mode" );
	}

	@Override
	public void parseInto(GraphImplementor<?> graph, String graphText, SessionFactoryImplementor sessionFactory) {
		final var parser = new ModernGraphLanguageParser( new CommonTokenStream( new ModernGraphLanguageLexer(
				CharStreams.fromString( graphText ) ) ) );
		final var graphCtx = parser.graph();
		final var visitor = new ModernGraphParser( sessionFactory );
		visitor.getGraphStack().push( graph );
		try {
			visitor.visitGraphElementList( graphCtx.graphElementList() );
		}
		finally {
			visitor.getGraphStack().pop();
			assert visitor.getGraphStack().isEmpty();
		}
	}

	public <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> rootType,
			ModernGraphLanguageParser.GraphElementListContext graphElementListContext,
			SessionFactoryImplementor sessionFactory) {
		return parse( null, rootType, graphElementListContext, new EntityNameResolverSessionFactory( sessionFactory ) );
	}


	public <T> RootGraphImplementor<T> parse(
			@Nullable String name,
			EntityDomainType<T> rootType,
			ModernGraphLanguageParser.GraphElementListContext graphElementListContext,
			EntityNameResolver entityNameResolver) {
		final RootGraphImpl<T> targetGraph = new RootGraphImpl<>( name, rootType );
		final var visitor = new ModernGraphParser( entityNameResolver );
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
