/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;

/**
 * A collection of {@link EntityGraph} utilities.
 *
 * @apiNote These methods really belong inside other classes that we cannot modify.
 *
 * @author asusnjar
 */
public final class EntityGraphs {
	/**
	 * Merges multiple entity graphs into a single graph that specifies the
	 * fetching/loading of all attributes the input graphs specify.
	 *
	 * @param <T> Root entity type of the query and graph.
	 *
	 * @param entityManager EntityManager to use to create the new merged graph.
	 * @param rootType Root type of the entity for which the graph is being merged.
	 * @param graphs Graphs to merge.
	 *
	 * @return The merged graph.
	 */
	@SafeVarargs
	public static <T> EntityGraph<T> merge(EntityManager entityManager, Class<T> rootType, EntityGraph<T>... graphs) {
		return mergeInternal( (SessionImplementor) entityManager, rootType, graphs );
	}

	@SafeVarargs
	public static <T> EntityGraph<T> merge(Session session, Class<T> rootType, Graph<T>... graphs) {
		return mergeInternal( (SessionImplementor) session, rootType, graphs );
	}

	@SafeVarargs
	public static <T> EntityGraph<T> merge(SessionImplementor session, Class<T> rootType, GraphImplementor<T>... graphs) {
		return mergeInternal( session, rootType, graphs );
	}

	private static <T> EntityGraph<T> mergeInternal(
			SessionImplementor session, Class<T> rootType, jakarta.persistence.Graph<T>[] graphs) {
		final RootGraphImplementor<T> merged = session.createEntityGraph( rootType );
		if ( graphs != null ) {
			for ( jakarta.persistence.Graph<T> graph : graphs ) {
				merged.merge( (GraphImplementor<T>) graph );
			}

		}
		return merged;
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing the query},
	 * applying the given {@link EntityGraph} using the specified semantic
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @deprecated Use {@link org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)}
	 */
	@Deprecated(since = "7.0")
	public static @SuppressWarnings("rawtypes") List executeList(Query query, EntityGraph<?> graph, GraphSemantic semantic) {
		return query.unwrap( org.hibernate.query.Query.class )
				.applyGraph( (RootGraph<?>) graph, semantic )
				.getResultList();
	}

	/**
	 * Form of {@link #executeList(Query, EntityGraph, GraphSemantic)} accepting a
	 * {@link TypedQuery}.
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @apiNote This signature assumes that the Query's return is an entity and that
	 *          the graph applies to that entity's type. JPA does not necessarily
	 *          require that, but it is by far the most common usage.
	 *
	 * @deprecated Use {@link org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)}
	 */
	@Deprecated(since = "7.0")
	public static <R> List<R> executeList(TypedQuery<R> query, EntityGraph<R> graph, GraphSemantic semantic) {
		@SuppressWarnings("unchecked")
		org.hibernate.query.Query<R> unwrapped = query.unwrap( org.hibernate.query.Query.class );
		return unwrapped.setEntityGraph( graph, semantic ).getResultList();
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing the query},
	 * applying the given {@link EntityGraph} using the named semantic using JPA's
	 * "hint name". See {@link GraphSemantic#fromHintName}.
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semanticJpaHintName See {@link GraphSemantic#fromHintName}
	 *
	 * @return The result list
	 *
	 * @deprecated Use {@link org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)}
	 */
	@Deprecated(since = "7.0")
	public static @SuppressWarnings("rawtypes") List executeList(Query query, EntityGraph<?> graph, String semanticJpaHintName) {
		return executeList( query, graph, GraphSemantic.fromHintName( semanticJpaHintName ) );
	}

	/**
	 * Form of {@link #executeList(Query, EntityGraph, String)} accepting a
	 * {@link TypedQuery}.
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semanticJpaHintName See {@link GraphSemantic#fromHintName}
	 *
	 * @apiNote This signature assumes that the Query's return is an entity and that
	 *          the graph applies to that entity's type. JPA does not necessarily
	 *          require that, but it is by far the most common usage.
	 *
	 * @deprecated Use {@link org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)}
	 */
	@Deprecated(since = "7.0")
	public static <R> List<R> executeList(TypedQuery<R> query, EntityGraph<R> graph, String semanticJpaHintName) {
		return executeList( query, graph, GraphSemantic.fromHintName( semanticJpaHintName ) );
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing the query}
	 * using the given {@link EntityGraph}.
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 *
	 * @apiNote Operates on the assumption that the "default" semantic for an
	 *          entity graph applied to a query is {@link GraphSemantic#FETCH}.
	 *          This is simply knowledge from JPA EG discussions, nothing that
	 *          is specifically mentioned or discussed in the spec.
	 *
	 * @deprecated Use {@link org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)}
	 */
	@Deprecated(since = "7.0")
	public static @SuppressWarnings("rawtypes") List executeList(Query query, EntityGraph<?> graph) {
		return executeList( query, graph, GraphSemantic.FETCH );
	}

	/**
	 * Form of {@link #executeList(Query, EntityGraph, String)} accepting a
	 * {@link TypedQuery}.
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 *
	 * @apiNote This signature assumes that the Query's return is an entity and that
	 *          the graph applies to that entity's type. JPA does not necessarily
	 *          require that, but it is by far the most common usage.
	 *
	 * @deprecated Use {@link org.hibernate.query.SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)}
	 */
	@Deprecated(since = "7.0")
	public static <R> List<R> executeList(TypedQuery<R> query, EntityGraph<R> graph) {
		return executeList( query, graph, GraphSemantic.FETCH );
	}

	/**
	 * Compares two entity graphs and returns {@code true} if they are equal,
	 * ignoring attribute order.
	 *
	 * @param <T>  Root entity type of BOTH graphs.
	 * @param a    Graph to compare.
	 * @param b    Graph to compare.
	 *
	 */
	public static <T> boolean areEqual(EntityGraph<T> a, EntityGraph<T> b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}

		final List<AttributeNode<?>> aNodes = a.getAttributeNodes();
		final List<AttributeNode<?>> bNodes = b.getAttributeNodes();

		if ( aNodes.size() != bNodes.size() ) {
			return false;
		}
		for ( AttributeNode<?> aNode : aNodes ) {
			String attributeName = aNode.getAttributeName();
			AttributeNode<?> bNode = null;
			for ( AttributeNode<?> bCandidate : bNodes ) {
				if ( attributeName.equals( bCandidate.getAttributeName() ) ) {
					bNode = bCandidate;
					break;
				}
			}
			if ( !areEqual( aNode, bNode ) ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Compares two entity graph attribute node and returns {@code true} if they are equal,
	 * ignoring subgraph attribute order.
	 */
	public static boolean areEqual(AttributeNode<?> a, AttributeNode<?> b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}
		if ( a.getAttributeName().equals( b.getAttributeName() ) ) {
			return areEqual( a.getSubgraphs(), b.getSubgraphs() )
				&& areEqual( a.getKeySubgraphs(), b.getKeySubgraphs() );
		}
		else {
			return false;
		}
	}

	/**
	 * Compares two entity subgraph maps and returns {@code true} if they are equal,
	 * ignoring order.
	 */
	public static boolean areEqual(
			@SuppressWarnings("rawtypes") Map<Class, Subgraph> a,
			@SuppressWarnings("rawtypes") Map<Class, Subgraph> b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}

		@SuppressWarnings("rawtypes")
		final Set<Class> aKeys = a.keySet();
		@SuppressWarnings("rawtypes")
		final Set<Class> bKeys = b.keySet();

		if ( aKeys.equals( bKeys ) ) {
			for ( Class<?> clazz : aKeys ) {
				if ( !bKeys.contains( clazz ) ) {
					return false;
				}
				if ( !areEqual( a.get( clazz ), b.get( clazz ) ) ) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Compares two entity subgraphs and returns {@code true} if they are equal,
	 * ignoring attribute order.
	 */
	public static boolean areEqual(@SuppressWarnings("rawtypes") Subgraph a, @SuppressWarnings("rawtypes") Subgraph b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}
		if ( a.getClassType() != b.getClassType() ) {
			return false;
		}

		@SuppressWarnings("unchecked")
		final List<AttributeNode<?>> aNodes = a.getAttributeNodes();
		@SuppressWarnings("unchecked")
		final List<AttributeNode<?>> bNodes = b.getAttributeNodes();

		if ( aNodes.size() != bNodes.size() ) {
			return false;
		}

		for ( AttributeNode<?> aNode : aNodes ) {
			final String attributeName = aNode.getAttributeName();
			AttributeNode<?> bNode = null;
			for ( AttributeNode<?> bCandidate : bNodes ) {
				if ( attributeName.equals( bCandidate.getAttributeName() ) ) {
					bNode = bCandidate;
					break;
				}
			}
			if ( !areEqual( aNode, bNode ) ) {
				return false;
			}
		}

		return true;
	}
}
