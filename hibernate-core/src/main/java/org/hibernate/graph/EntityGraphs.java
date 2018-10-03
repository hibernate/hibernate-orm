/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;

/**
 * A collection of {@link EntityGraph} utilities.
 * These methods really belong inside other classes that we cannot modify, hence here.
 * 
 * @author asusnjar
 */
@SuppressWarnings("WeakerAccess")
public final class EntityGraphs {
	/**
	 * Merges multiple entity graphs into a single graph that specifies the fetching/loading of all attributes the input
	 * graphs specify.
	 * 
	 * @param <T>      Root entity type of the query and graph.
	 * 
	 * @param em       EntityManager to use to create the new merged graph.
	 * @param rootType Root type of the entity for which the graph is being merged.
	 * @param graphs   Graphs to merge.
	 * 
	 * @return         The merged graph.
	 */
	@SuppressWarnings("unchecked")
	public static <T> EntityGraph<T> merge(EntityManager em, Class<T> rootType, EntityGraph<T>... graphs) {
		return merge( (SessionImplementor) em, rootType, (Object[]) graphs );
	}

	@SafeVarargs
	public static <T> EntityGraph<T> merge(Session session, Class<T> rootType, Graph<T>... graphs) {
		return merge( (SessionImplementor) session, rootType, (Object[]) graphs );
	}

	@SafeVarargs
	public static <T> EntityGraph<T> merge(SessionImplementor session, Class<T> rootType, GraphImplementor<T>... graphs) {
		return merge( session, rootType, (Object[]) graphs );
	}

	@SuppressWarnings("unchecked")
	private static <T> EntityGraph<T> merge(SessionImplementor session, Class<T> rootType, Object... graphs) {
		RootGraphImplementor<T> merged = session.createEntityGraph( rootType );

		if ( graphs != null ) {
			for ( Object graph : graphs ) {
				merged.merge( (GraphImplementor<T>) graph );
			}

		}

		return merged;
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing} the Query, applying the
	 * given EntityGraph using the specified semantic
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semantic The semantic to use when applying the graph
	 */
	@SuppressWarnings("unchecked")
	public static List executeList(Query query, EntityGraph graph, GraphSemantic semantic) {
		return query.unwrap( org.hibernate.query.Query.class )
				.applyGraph( (RootGraph) graph, semantic )
				.list();
	}

	/**
	 * Form of {@link #executeList(Query, EntityGraph, GraphSemantic)} accepting a TypedQuery.
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @apiNote This signature assumes that the Query's return is an entity and that the graph
	 * applies to that entity's type.  JPA does not necessarily require that, but it is by
	 * far the most common usage.
	 */
	@SuppressWarnings({"unused", "unchecked"})
	public static <R> List<R> executeList(TypedQuery<R> query, EntityGraph<R> graph, GraphSemantic semantic) {
		return executeList( (Query) query, graph, semantic );
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing} the Query, applying the
	 * given EntityGraph using the named semantic using JPA's "hint name" - see
	 * {@link GraphSemantic#fromJpaHintName}
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semanticJpaHintName See {@link GraphSemantic#fromJpaHintName}
	 *
	 * @return The result list
	 */
	@SuppressWarnings({"unused", "unchecked"})
	public static List executeList(Query query, EntityGraph graph, String semanticJpaHintName) {
		return query.unwrap( org.hibernate.query.Query.class )
				.applyGraph( (RootGraph) graph, GraphSemantic.fromJpaHintName( semanticJpaHintName ) )
				.list();
	}

	/**
	 * Form of {@link #executeList(Query, EntityGraph, String)} accepting a TypedQuery
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 * @param semanticJpaHintName See {@link GraphSemantic#fromJpaHintName}
	 *
	 * @apiNote This signature assumes that the Query's return is an entity and that the graph
	 * applies to that entity's type.  JPA does not necessarily require that, but it is by
	 * far the most common usage.
	 */
	@SuppressWarnings({"unused", "unchecked"})
	public static <R> List<R> executeList(TypedQuery<R> query, EntityGraph<R> graph, String semanticJpaHintName) {
		return executeList( (Query) query, graph, semanticJpaHintName );
	}

	/**
	 * Convenience method for {@linkplain Query#getResultList() executing} the Query using the
	 * given EntityGraph
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 *
	 * @apiNote operates on the assumption that the "default" semantic for an
	 * entity graph applied to a Query is {@link GraphSemantic#FETCH}.  This is simply
	 * knowledge from JPA EG discussions, nothing that is specifically mentioned or
	 * discussed in the spec.
	 */
	@SuppressWarnings({"unused", "unchecked"})
	public static List executeList(Query query, EntityGraph graph) {
		return query.unwrap( org.hibernate.query.Query.class )
				.applyFetchGraph( (RootGraph) graph )
				.list();
	}

	/**
	 * Form of {@link #executeList(Query, EntityGraph, String)} accepting a TypedQuery
	 *
	 * @param query The JPA Query
	 * @param graph The graph to apply
	 *
	 * @apiNote This signature assumes that the Query's return is an entity and that the graph
	 * applies to that entity's type.  JPA does not necessarily require that, but it is by
	 * far the most common usage.
	 */
	@SuppressWarnings("unused")
	public static <R> List<R> executeList(TypedQuery<R> query, EntityGraph<R> graph) {
		return executeList( query, graph, GraphSemantic.FETCH );
	}

	// todo : ? - we could add JPA's other Query execution methods
	//		but really, I think unwrapping as Hibernate's Query and using our
	//		"proprietary" methods is better (this class is "proprietary" too).


	/**
	 * Compares two entity graphs and returns {@code true} if they are equal, ignoring attribute order.
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

		List<AttributeNode<?>> aNodes = a.getAttributeNodes();
		List<AttributeNode<?>> bNodes = b.getAttributeNodes();

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
	 * Compares two entity graph attribute node and returns {@code true} if they are equal, ignoring subgraph attribute
	 * order.
	 */
	public static boolean areEqual(AttributeNode<?> a, AttributeNode<?> b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}
		if ( a.getAttributeName().equals( b.getAttributeName() ) ) {
			return areEqual( a.getSubgraphs(), b.getSubgraphs() ) && areEqual( a.getKeySubgraphs(), b.getKeySubgraphs() );
		}
		else {
			return false;
		}
	}

	/**
	 * Compares two entity subgraph maps and returns {@code true} if they are equal, ignoring order.
	 */
	public static boolean areEqual(@SuppressWarnings("rawtypes") Map<Class, Subgraph> a, @SuppressWarnings("rawtypes") Map<Class, Subgraph> b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}

		@SuppressWarnings("rawtypes")
		Set<Class> aKeys = a.keySet();
		@SuppressWarnings("rawtypes")
		Set<Class> bKeys = b.keySet();

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
	 * Compares two entity subgraphs and returns {@code true} if they are equal, ignoring attribute order.
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
		List<AttributeNode<?>> aNodes = a.getAttributeNodes();
		@SuppressWarnings("unchecked")
		List<AttributeNode<?>> bNodes = b.getAttributeNodes();

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
}
