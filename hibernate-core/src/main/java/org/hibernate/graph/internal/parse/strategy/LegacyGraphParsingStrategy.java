/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse.strategy;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.legacy.GraphLanguageLexer;
import org.hibernate.grammars.graph.legacy.GraphLanguageParser;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.internal.parse.EntityNameResolver;
import org.hibernate.graph.internal.parse.EntityNameResolverSessionFactory;
import org.hibernate.graph.internal.parse.LegacyGraphParser;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

@Deprecated(forRemoval = true)
public class LegacyGraphParsingStrategy implements GraphParsingStrategy {

	@Override
	public <T> RootGraphImplementor<T> parse(Class<T> entityClass, String graphText, SessionFactoryImplementor sessionFactory) {
		if (graphText == null) return null;
		final var parser = new GraphLanguageParser(new CommonTokenStream(new GraphLanguageLexer(CharStreams.fromString(graphText))));
		final var graph = parser.graph();
		if (graph.typeIndicator() != null) {
			throw new InvalidGraphException("Expecting graph text to not include an entity name : " + graphText);
		}
		final EntityDomainType<T> entityType = sessionFactory.getJpaMetamodel().entity(entityClass);
		return parse(entityType, graph.attributeList(), sessionFactory);
	}

	@Override
	public <T> RootGraphImplementor<T> parse(EntityDomainType<T> entityDomainType, String graphText, SessionFactoryImplementor sessionFactory) {
		if (graphText == null) return null;
		final var parser = new GraphLanguageParser(new CommonTokenStream(new GraphLanguageLexer(CharStreams.fromString(graphText))));
		final var graph = parser.graph();
		if (graph.typeIndicator() != null) {
			throw new InvalidGraphException("Expecting graph text to not include an entity name : " + graphText);
		}
		return parse(entityDomainType, graph.attributeList(), sessionFactory);
	}

	@Override
	public <T> RootGraphImplementor<T> parse(String entityName, String graphText, SessionFactoryImplementor sessionFactory) {
		if (graphText == null) return null;
		final var parser = new GraphLanguageParser(new CommonTokenStream(new GraphLanguageLexer(CharStreams.fromString(graphText))));
		final var graph = parser.graph();
		if (graph.typeIndicator() != null) {
			throw new InvalidGraphException("Expecting graph text to not include an entity name : " + graphText);
		}
		@SuppressWarnings("unchecked")
		final EntityDomainType<T> entityType = (EntityDomainType<T>) sessionFactory.getJpaMetamodel().entity(entityName);
		return parse(entityType, graph.attributeList(), sessionFactory);
	}

	@Override
	@Deprecated(forRemoval = true)
	public <T> RootGraphImplementor<T> parse(String graphText, SessionFactoryImplementor sessionFactory) {
		if (graphText == null) return null;
		final var parser = new GraphLanguageParser(new CommonTokenStream(new GraphLanguageLexer(CharStreams.fromString(graphText))));
		final var graph = parser.graph();
		if (graph.typeIndicator() == null) {
			throw new InvalidGraphException("Expecting graph text to include an entity name : " + graphText);
		}
		final String entityName = graph.typeIndicator().TYPE_NAME().getText();
		@SuppressWarnings("unchecked")
		final EntityDomainType<T> entityType = (EntityDomainType<T>) sessionFactory.getJpaMetamodel().entity(entityName);
		return parse(entityType, graph.attributeList(), sessionFactory);
	}

	@Override
	public void parseInto(GraphImplementor<?> graph, String graphText, SessionFactoryImplementor sessionFactory) {
		final GraphLanguageLexer lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final GraphLanguageParser parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final GraphLanguageParser.GraphContext graphContext = parser.graph();

		if ( graphContext.typeIndicator() != null ) {
			// todo : throw an exception?  Log warning?  Ignore?
			//		for now, ignore
		}

		// Build an instance of this class as a visitor
		final LegacyGraphParser visitor = new LegacyGraphParser( sessionFactory );

		visitor.getGraphStack().push( graph );
		try {
			visitor.visitAttributeList( graphContext.attributeList() );
		}
		finally {
			visitor.getGraphStack().pop();

			assert visitor.getGraphStack().isEmpty();
		}

	}

	public <T> RootGraphImplementor<T> parse(EntityDomainType<T> rootType, GraphLanguageParser.AttributeListContext graphElementListContext, SessionFactoryImplementor sessionFactory) {
		return parse(null, rootType, graphElementListContext, new EntityNameResolverSessionFactory( sessionFactory));
	}

	public <T> RootGraphImplementor<T> parse(@Nullable String name, EntityDomainType<T> rootType, GraphLanguageParser.AttributeListContext graphElementListContext, EntityNameResolver entityNameResolver) {
		final RootGraphImpl<T> targetGraph = new RootGraphImpl<>(name, rootType);

		final var visitor = new LegacyGraphParser( entityNameResolver);
		visitor.getGraphStack().push(targetGraph);
		try {
			visitor.visitAttributeList(graphElementListContext);
		}
		finally {
			visitor.getGraphStack().pop();
			assert visitor.getGraphStack().isEmpty();
		}
		return targetGraph;
	}
}
