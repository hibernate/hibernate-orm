/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Graph;
import jakarta.persistence.Query;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;

import jakarta.persistence.metamodel.EntityType;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.SelectionQuery;

/**
 * A collection of {@link EntityGraph} utilities.
 *
 * @apiNote These operations are things which are arguably missing from JPA.
 *
 * @author asusnjar
 * @author Gavin King
 */
public final class EntityGraphs {

	/**
	 * Create a new entity graph rooted at the given entity, without
	 * needing a reference to the session or session factory.
	 *
	 * @param rootType The {@link EntityType} representing the root
	 *                 entity of the graph
	 * @return a new mutable {@link EntityGraph}
	 *
	 * @since 7.0
	 */
	public static <T> EntityGraph<T> createGraph(EntityType<T> rootType) {
		return new RootGraphImpl<>( null, (EntityDomainType<T>) rootType );
	}

	/**
	 * Create a new entity graph rooted at the given
	 * {@linkplain RepresentationMode#MAP dynamic entity}, without
	 * needing a reference to the session or session factory.
	 *
	 * @param rootType The {@link EntityType} representing the root
	 *                 entity of the graph, which must be a dynamic
	 *                 entity
	 * @return a new mutable {@link EntityGraph}
	 *
	 * @since 7.0
	 */
	public static EntityGraph<Map<String,?>> createGraphForDynamicEntity(EntityType<?> rootType) {
		final EntityDomainType<?> domainType = (EntityDomainType<?>) rootType;
		if ( domainType.getRepresentationMode() != RepresentationMode.MAP ) {
			throw new IllegalArgumentException( "Entity '" + domainType.getHibernateEntityName()
												+ "' is not a dynamic entity" );
		}
		@SuppressWarnings("unchecked") //Safe, because we just checked
		final EntityDomainType<Map<String, ?>> dynamicEntity = (EntityDomainType<Map<String, ?>>) domainType;
		return new RootGraphImpl<>( null, dynamicEntity );
	}

	/**
	 * Merges multiple entity graphs into a single graph that specifies the
	 * fetching/loading of all attributes the input graphs specify.
	 *
	 * @param <T> Root entity type of the query and graph.
	 *
	 * @param entityManager {@code EntityManager} to use to create the new merged graph.
	 * @param root Root type of the entity for which the graph is being merged.
	 * @param graphs Graphs to merge.
	 *
	 * @return The merged graph.
	 */
	@SafeVarargs
	public static <T> EntityGraph<T> merge(EntityManager entityManager, Class<T> root, Graph<T>... graphs) {
		return merge( entityManager, root, Arrays.stream(graphs) );
	}

	/**
	 * Merges multiple entity graphs into a single graph that specifies the
	 * fetching/loading of all attributes the input graphs specify.
	 *
	 * @param <T> Root entity type of the query and graph.
	 *
	 * @param entityManager {@code EntityManager} to use to create the new merged graph.
	 * @param root Root type of the entity for which the graph is being merged.
	 * @param graphs Graphs to merge.
	 *
	 * @return The merged graph.
	 *
	 * @since 7.0
	 */
	public static <T> EntityGraph<T> merge(EntityManager entityManager, Class<T> root, List<? extends Graph<T>> graphs) {
		return merge( entityManager, root, graphs.stream() );
	}

	/**
	 * Merges multiple entity graphs into a single graph that specifies the
	 * fetching/loading of all attributes the input graphs specify.
	 *
	 * @param <T> Root entity type of the query and graph.
	 *
	 * @param entityManager {@code EntityManager} to use to create the new merged graph.
	 * @param root Root type of the entity for which the graph is being merged.
	 * @param graphs Graphs to merge.
	 *
	 * @return The merged graph.
	 *
	 * @since 7.0
	 */
	public static <T> EntityGraph<T> merge(EntityManager entityManager, Class<T> root, Stream<? extends Graph<T>> graphs) {
		final RootGraphImplementor<T> merged = ((SessionImplementor) entityManager).createEntityGraph( root );
		graphs.forEach( graph -> merged.merge( (GraphImplementor<T>) graph ) );
		return merged;
	}

	/**
	 * Convenience method to apply the given graph to the given query
	 * without the need for a cast when working with JPA API.
	 *
	 * @param query The JPA {@link TypedQuery}
	 * @param graph The JPA {@link EntityGraph} to apply
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @see SelectionQuery#setEntityGraph(EntityGraph, GraphSemantic)
	 *
	 * @since 7.0
	 */
	public static <R> void setGraph(TypedQuery<R> query, EntityGraph<R> graph, GraphSemantic semantic) {
		((org.hibernate.query.Query<R>) query).setEntityGraph( graph, semantic );
	}

	/**
	 * Convenience method to apply the given load graph to the given
	 * query without the need for a cast when working with JPA API.
	 *
	 * @param query The JPA {@link TypedQuery}
	 * @param graph The JPA {@link EntityGraph} to apply
	 *
	 * @since 7.0
	 */
	public static <R> void setLoadGraph(TypedQuery<R> query, EntityGraph<R> graph) {
		setGraph( query, graph, GraphSemantic.LOAD );
	}

	/**
	 * Convenience method to apply the given fetch graph to the given
	 * query without the need for a cast when working with JPA API.
	 *
	 * @param query The JPA {@link TypedQuery}
	 * @param graph The JPA {@link EntityGraph} to apply
	 *
	 * @since 7.0
	 */
	public static <R> void setFetchGraph(TypedQuery<R> query, EntityGraph<R> graph) {
		setGraph( query, graph, GraphSemantic.FETCH );
	}

	/**
	 * Allows a treated subgraph to ve created for a {@link Subgraph}, since the
	 * JPA-standard operation {@link EntityGraph#addTreatedSubgraph(Class)} is
	 * declared by {@link EntityGraph}.
	 *
	 * @param graph any {@linkplain Graph root graph or subgraph}
	 * @param subtype the treated (narrowed) type
	 *
	 * @since 7.0
	 */
	public <S> Subgraph<S> addTreatedSubgraph(Graph<? super S> graph, Class<S> subtype) {
		return ((org.hibernate.graph.Graph<? super S>) graph).addTreatedSubgraph( subtype );
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing the query},
	 * applying the given {@link EntityGraph} using the specified semantic
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @deprecated Since it is not type safe and returns a raw type
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
	 * @deprecated Use {@link #setGraph(TypedQuery, EntityGraph, GraphSemantic)} instead
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
	 * @deprecated Since it is not type safe, returns a raw type, and accepts a string
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
	 * @deprecated Since it accepts a string instead of {@link GraphSemantic}
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
	 * @deprecated Since it is not type safe and returns a raw type
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
	 * @deprecated Use {@link #setFetchGraph(TypedQuery, EntityGraph)} instead
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
	public static boolean areEqual(
			@SuppressWarnings("rawtypes") Subgraph a,
			@SuppressWarnings("rawtypes") Subgraph b) {
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
