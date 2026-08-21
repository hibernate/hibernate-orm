/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Subgraph;

import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.internal.parse.GraphParsing;
import org.hibernate.graph.spi.GraphImplementor;

/**
 * Parser for string representations of {@linkplain RootGraph entity graphs}.
 * The syntax is<pre>
 *     graph:: (rootEntityName COLON)? attributeList
 *     attributeList:: attributeNode (COMMA attributeNode)*
 *     attributeNode:: attributePath subGraph?
 *     subGraph:: LPAREN (subTypeEntityName COLON)? attributeList RPAREN
 * </pre>
 * <p/>
 * The {@link #parse} methods all create a root {@link jakarta.persistence.EntityGraph}
 * based on the passed entity class and parse the graph string into that root graph.
 * <p>
 * The {@link #parseInto} methods parse the graph string into a passed graph, which may
 * be a subgraph.
 * <p>
 * Multiple graphs for the same entity type can be
 * {@linkplain EntityGraphs#merge(EntityManager, Class, jakarta.persistence.Graph...)
 * merged}.
 *
 * @author asusnjar
 */
public final class GraphParser {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parse (creation)

	/**
	 * Creates a root graph based on the passed {@code rootEntityClass} and parses
	 * {@code graphText} into the generated root graph.
	 *
	 * @param rootEntityClass The entity class to use as the graph root
	 * @param graphText The textual representation of the graph
	 * @param sessionFactory The SessionFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 *
	 * @apiNote The string representation is expected to just be an attribute list, without
	 * the entity-type prefix.  E.g. {@code "title, isbn, author(name, books)"}
	 *
	 * @see org.hibernate.SessionFactory#parseEntityGraph(Class, CharSequence)
	 *
	 * @since 7.0
	 */
	public static <T> RootGraph<T> parse(
			final Class<T> rootEntityClass,
			final CharSequence graphText,
			final SessionFactory sessionFactory) {
		if ( graphText == null ) {
			return null;
		}
		return GraphParsing.parse(
				rootEntityClass,
				graphText.toString(),
				sessionFactory.unwrap( SessionFactoryImplementor.class )
		);
	}

	/**
	 * Creates a root graph based on the passed {@code rootEntityName} and parses
	 * {@code graphText} into the generated root graph.
	 *
	 * @param rootEntityName The name of the entity to use as the graph root
	 * @param graphText The textual representation of the graph
	 * @param sessionFactory The SessionFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 *
	 * @apiNote The string representation is expected to just be an attribute list, without
	 * the entity-type prefix.  E.g. {@code "title, isbn, author(name, books)"}
	 *
	 * @see org.hibernate.SessionFactory#parseEntityGraph(Class, CharSequence)
	 *
	 * @since 7.0
	 */
	@Incubating
	public static <T> RootGraph<T> parse(
			final String rootEntityName,
			final CharSequence graphText,
			final SessionFactory sessionFactory) {
		if ( graphText == null ) {
			return null;
		}
		//noinspection unchecked
		return (RootGraph<T>) GraphParsing.parse(
				rootEntityName,
				graphText.toString(),
				sessionFactory.unwrap( SessionFactoryImplementor.class )
		);
	}

	/**
	 * Creates a root graph based on the passed {@code graphText}.  The format of this
	 * text is the root name with a colon, followed by an attribute list.
	 * E.g. {@code "Book: title, isbn, author(name, books)"}.
	 *
	 * @param graphText The textual representation of the graph
	 * @param sessionFactory The SessionFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 *
	 * @see org.hibernate.SessionFactory#parseEntityGraph(Class, CharSequence)
	 *
	 * @since 7.0
	 */
	@Incubating
	public static <T> RootGraph<T> parse(
				final CharSequence graphText,
				final SessionFactory sessionFactory) {
			if ( graphText == null ) {
				return null;
			}
			//noinspection unchecked
			return (RootGraph<T>) GraphParsing.parse(
					graphText.toString(),
					sessionFactory.unwrap( SessionFactoryImplementor.class )
			);
	}

	/**
	 * Creates a root graph based on the passed {@code rootType} and parses {@code graphText}
	 * into the generated root graph.
	 *
	 * @apiNote The passed EntityManager is expected to be a Hibernate implementation.
	 * Attempting to pass another provider's EntityManager implementation will fail.
	 * @implNote Simply delegates to {@linkplain #parse(Class, CharSequence, SessionFactory)}
	 *
	 * @param rootType The root entity type
	 * @param graphText The textual representation of the graph
	 * @param entityManager The EntityManager
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> RootGraph<T> parse(
			final Class<T> rootType,
			final CharSequence graphText,
			final EntityManager entityManager) {
		if ( graphText == null ) {
			return null;
		}
		return GraphParsing.parse(
				rootType,
				graphText.toString(),
				entityManager.getEntityManagerFactory()
						.unwrap( SessionFactoryImplementor.class )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parse (into)

	/**
	 * Parses the textual graph representation  into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated by this process
	 * @param graphText Textual representation of the graph
	 * @param entityManager The EntityManager
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> void parseInto(
			final Graph<T> graph,
			final CharSequence graphText,
			final EntityManager entityManager) {
		parseInto(
				(GraphImplementor<T>) graph,
				graphText,
				( (SessionImplementor) entityManager ).getSessionFactory()
		);
	}

	/**
	 * Parses the textual graph representation  into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated by this process
	 * @param graphText Textual representation of the graph
	 * @param entityManager The EntityManager
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> void parseInto(
			final EntityGraph<?> graph,
			final CharSequence graphText,
			final EntityManager entityManager) {
		parseInto(
				(GraphImplementor<?>) graph,
				graphText,
				( (SessionImplementor) entityManager ).getSessionFactory()
		);
	}

	/**
	 * Parses the textual graph representation  into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated by this process
	 * @param graphText Textual representation of the graph
	 * @param entityManager The EntityManager
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static void parseInto(
			final Subgraph<?> graph,
			final CharSequence graphText,
			final EntityManager entityManager) {
		parseInto(
				(GraphImplementor<?>) graph,
				graphText,
				( (SessionImplementor) entityManager ).getSessionFactory()
		);
	}

	/**
	 * Parses the textual graph representation  into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated by this process
	 * @param graphText Textual representation of the graph
	 * @param entityManagerFactory The EntityManagerFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static void parseInto(
			final Graph<?> graph,
			final CharSequence graphText,
			final EntityManagerFactory entityManagerFactory) {
		parseInto(
				(GraphImplementor<?>) graph,
				graphText,
				(SessionFactoryImplementor) entityManagerFactory
		);
	}

	/**
	 * Parses the textual graph representation  into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated by this process
	 * @param graphText Textual representation of the graph
	 * @param entityManagerFactory The EntityManagerFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static void parseInto(
			final EntityGraph<?> graph,
			final CharSequence graphText,
			final EntityManagerFactory entityManagerFactory) {
		parseInto(
				(GraphImplementor<?>) graph,
				graphText,
				(SessionFactoryImplementor) entityManagerFactory
		);
	}

	/**
	 * Parses the textual graph representation  into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated by this process
	 * @param graphText Textual representation of the graph
	 * @param entityManagerFactory The EntityManagerFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static void parseInto(
			final Subgraph<?> graph,
			final CharSequence graphText,
			final EntityManagerFactory entityManagerFactory) {
		parseInto(
				(GraphImplementor<?>) graph,
				graphText,
				(SessionFactoryImplementor) entityManagerFactory
		);
	}

	/**
	 * Parses the textual graph representation as {@linkplain GraphParser described above}
	 * into the specified graph.
	 *
	 * @param graph The target graph.  This is the graph that will be populated
	 * by this process
	 * @param graphText Textual representation of the graph
	 * @param sessionFactory The SessionFactory reference
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	private static void parseInto(
			GraphImplementor<?> graph,
			final CharSequence graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText != null ) {
			GraphParsing.parseInto( graph, graphText, sessionFactory );
		}
	}

}
