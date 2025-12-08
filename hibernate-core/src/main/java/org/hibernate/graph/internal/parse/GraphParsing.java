/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.GraphLanguageLexer;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.internal.RootGraphImpl;
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
			Class<T> entityClass,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}

		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final var graphContext = parser.graph();

		if ( graphContext.typeIndicator() != null ) {
			// todo : an alternative here would be to simply validate that the entity type
			//  	from the text matches the passed one...
			throw new InvalidGraphException( "Expecting graph text to not include an entity name : " + graphText );
		}

		final var entityType = sessionFactory.getJpaMetamodel().entity( entityClass );
		return parse( entityType, graphContext.attributeList(), sessionFactory );
	}

	public static <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> entityDomainType,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}

		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final var graphContext = parser.graph();

		if ( graphContext.typeIndicator() != null ) {
			// todo : an alternative here would be to simply validate that the entity type
			//  	from the text matches the passed one...
			throw new InvalidGraphException( "Expecting graph text to not include an entity name : " + graphText );
		}

		return parse( entityDomainType, graphContext.attributeList(), sessionFactory );
	}

	public static RootGraphImplementor<?> parse(
			String entityName,
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}

		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final var graphContext = parser.graph();

		if ( graphContext.typeIndicator() != null ) {
			// todo : an alternative here would be to simply validate that the entity type
			//  	from the text matches the passed one...
			throw new InvalidGraphException( "Expecting graph text to not include an entity name : " + graphText );
		}

		final var entityType = sessionFactory.getJpaMetamodel().entity( entityName );
		return parse( entityType, graphContext.attributeList(), sessionFactory );
	}

	public static RootGraphImplementor<?> parse(
			String graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText == null ) {
			return null;
		}

		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphText ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final var graphContext = parser.graph();

		if ( graphContext.typeIndicator() == null ) {
			throw new InvalidGraphException( "Expecting graph text to include an entity name : " + graphText );
		}

		final String entityName = graphContext.typeIndicator().TYPE_NAME().getText();
		final var entityType = sessionFactory.getJpaMetamodel().entity( entityName );
		return parse( entityType, graphContext.attributeList(), sessionFactory );
	}

	public static <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> rootType,
			GraphLanguageParser.AttributeListContext attributeListContext,
			SessionFactoryImplementor sessionFactory) {
		return parse( rootType, attributeListContext, sessionFactory.getJpaMetamodel()::findEntityType );
	}

	public static <T> RootGraphImplementor<T> parse(
			EntityDomainType<T> rootType,
			GraphLanguageParser.AttributeListContext attributeListContext,
			EntityNameResolver entityNameResolver) {
		return parse( null, rootType, attributeListContext, entityNameResolver );
	}

	public static <T> RootGraphImplementor<T> parse(
			@Nullable String name,
			EntityDomainType<T> rootType,
			GraphLanguageParser.AttributeListContext attributeListContext,
			EntityNameResolver entityNameResolver) {
		final RootGraphImpl<T> targetGraph = new RootGraphImpl<>( name, rootType );

		final var visitor = new GraphParser( entityNameResolver );
		visitor.getGraphStack().push( targetGraph );
		try {
			visitor.visitAttributeList( attributeListContext );
		}
		finally {
			visitor.getGraphStack().pop();

			assert visitor.getGraphStack().isEmpty();
		}

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
		final var lexer = new GraphLanguageLexer( CharStreams.fromString( graphString.toString() ) );
		final var parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final var graphContext = parser.graph();

		if ( graphContext.typeIndicator() != null ) {
			// todo : throw an exception?  Log warning?  Ignore?
			//		for now, ignore
		}

		// Build an instance of this class as a visitor
		final var visitor = new GraphParser( sessionFactory );

		visitor.getGraphStack().push( targetGraph );
		try {
			visitor.visitAttributeList( graphContext.attributeList() );
		}
		finally {
			visitor.getGraphStack().pop();

			assert visitor.getGraphStack().isEmpty();
		}
	}
}
