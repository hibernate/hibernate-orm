/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.GraphLanguageLexer;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.grammars.graph.GraphLanguageParser.GraphContext;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.internal.RootGraphImpl;
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
		if ( graphText == null ) {
			return null;
		}

		final var graphContext = parseText( graphText );
		if ( graphContext.typeIndicator() != null ) {
			// todo : an alternative here would be to simply validate that the entity type
			//  	from the text matches the passed one...
			throw new InvalidGraphException( "Expecting graph text to not include an entity name: " + graphText );
		}

		return visit( entityDomainType, graphContext.attributeList(), sessionFactory );
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

	public static RootGraphImplementor<?> parse(
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}

		final var graphContext = parseText( graphText );
		if ( graphContext.typeIndicator() == null ) {
			throw new InvalidGraphException( "Expecting graph text to include an entity name: " + graphText );
		}

		final String entityName = graphContext.typeIndicator().TYPE_NAME().getText();
		final var entityType = sessionFactory.getJpaMetamodel().entity( entityName );
		return visit( entityType, graphContext.attributeList(), sessionFactory );
	}

	public static <T> RootGraphImplementor<T> visit(
			EntityDomainType<T> rootType,
			GraphLanguageParser.AttributeListContext attributeListContext,
			SessionFactoryImplementor sessionFactory) {
		return visit( rootType, attributeListContext, sessionFactory.getJpaMetamodel()::findEntityType );
	}

	public static <T> RootGraphImplementor<T> visit(
			EntityDomainType<T> rootType,
			GraphLanguageParser.AttributeListContext attributeListContext,
			GraphParserEntityNameResolver entityNameResolver) {
		return visit( null, rootType, attributeListContext, entityNameResolver );
	}

	public static @NonNull GraphContext parseText(String graphText) {
		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		return parser.graph();
	}

	public static <T> RootGraphImplementor<T> visit(
			@Nullable String name,
			EntityDomainType<T> rootType,
			GraphLanguageParser.AttributeListContext attributeListContext,
			GraphParserEntityNameResolver entityNameResolver) {
		final RootGraphImpl<T> targetGraph = new RootGraphImpl<>( name, rootType );
		visitGraph( targetGraph, entityNameResolver, attributeListContext );
		return targetGraph;
	}

	/**
	 * Parse the passed graph textual representation into the passed Graph.
	 * Essentially overlays the text representation on top of the graph.
	 */
	public static void parseInto(
			GraphImplementor<?> targetGraph,
			CharSequence graphString,
			SessionFactoryImplementor sessionFactory) {
		final var graphContext = parseText( graphString.toString() );
		if ( graphContext.typeIndicator() != null ) {
			// todo : throw an exception?  Log warning?  Ignore?
			//		for now, ignore
		}
		visitGraph( targetGraph,
				sessionFactory.getJpaMetamodel()::findEntityType,
				graphContext.attributeList() );
	}

	private static void visitGraph(
			GraphImplementor<?> targetGraph,
			GraphParserEntityNameResolver entityNameResolver,
			GraphLanguageParser.AttributeListContext attributeList) {
		// Build an instance of this class as a visitor
		final var visitor = new GraphParser( entityNameResolver );
		visitor.getGraphStack().push( targetGraph );
		try {
			visitor.visitAttributeList( attributeList );
		}
		finally {
			visitor.getGraphStack().pop();
			assert visitor.getGraphStack().isEmpty();
		}
	}
}
