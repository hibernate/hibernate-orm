/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Subgraph;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;

/**
 * Parser for string representations of JPA {@link javax.persistence.EntityGraph}
 * ({@link RootGraph}) and {@link javax.persistence.Subgraph} ({@link SubGraph}),
 * using a simple syntax defined by the `graph.g` Antlr grammar.
 * <p/>
 * The {@link #parse} methods all create a root {@link javax.persistence.EntityGraph}
 * based on the passed entity class and parse the graph string into that root graph.
 * <p/>
 * The {@link #parseInto} methods parse the graph string into a passed graph, which may be a sub-graph
 * <p/>
 * Multiple graphs made for the same entity type can be merged.  See {@link EntityGraphs#merge}.
 *
 * @author asusnjar
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class GraphParser {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parse (creation)

	/**
	 * Creates a root graph based on the passed `rootType` and parses `graphText` into
	 * the generated root graph
	 *
	 * @apiNote The passed EntityManager is expected to be a Hibernate implementation.
	 * Attempting to pass another provider's EntityManager implementation will fail
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
		return parse( rootType, graphText, (SessionImplementor) entityManager );
	}

	private static <T> RootGraphImplementor<T> parse(
			final Class<T> rootType,
			final CharSequence graphText,
			final SessionImplementor session) {
		if ( graphText == null ) {
			return null;
		}

		final RootGraphImplementor<T> graph = session.createEntityGraph( rootType );
		parseInto( (GraphImplementor<T>) graph, graphText, session.getSessionFactory() );
		return graph;
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
	@SuppressWarnings("unchecked")
	public static <T> void parseInto(
			final EntityGraph<T> graph,
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
	@SuppressWarnings("unchecked")
	public static <T> void parseInto(
			final Subgraph<T> graph,
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
	 * @param entityManagerFactory The EntityManagerFactory
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> void parseInto(
			final Graph<T> graph,
			final CharSequence graphText,
			final EntityManagerFactory entityManagerFactory) {
		parseInto(
				(GraphImplementor<T>) graph,
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
	@SuppressWarnings("unchecked")
	public static <T> void parseInto(
			final EntityGraph<T> graph,
			final CharSequence graphText,
			final EntityManagerFactory entityManagerFactory) {
		parseInto(
				(GraphImplementor<T>) graph,
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
	@SuppressWarnings("unchecked")
	public static <T> void parseInto(
			final Subgraph<T> graph,
			final CharSequence graphText,
			final EntityManagerFactory entityManagerFactory) {
		parseInto(
				(GraphImplementor<T>) graph,
				graphText,
				(SessionFactoryImplementor) entityManagerFactory
		);
	}

	/**
	 * Parses the textual graph representation as {@linkplain GraphParser described above}
	 * into the specified graph.
	 *
	 * @param <T> The Java type for the ManagedType described by `graph`
	 *
	 * @param graph The target graph.  This is the graph that will be populated
	 * by this process
	 * @param graphText Textual representation of the graph
	 * @param sessionFactory The SessionFactory reference
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 */
	private static <T> void parseInto(
			GraphImplementor<T> graph,
			final CharSequence graphText,
			SessionFactoryImplementor sessionFactory) {
		if ( graphText != null ) {
			org.hibernate.graph.internal.parse.GraphParser.parseInto( graph, graphText, sessionFactory );
		}
	}

}
