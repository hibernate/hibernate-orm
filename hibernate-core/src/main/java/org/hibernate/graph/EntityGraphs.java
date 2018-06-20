/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.graph;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;

/**
 * A collection of {@link EntityGraph} utilities.
 * These methods really belong inside other classes that we cannot modify, hence here.
 * 
 * @author asusnjar
 */
public final class EntityGraphs {

	public static String HINT_FETCHGRAPH = "javax.persistence.fetchgraph";
	public static String HINT_LOADGRAPH = "javax.persistence.loadgraph";

	/**
	 * Configures the query hints so that ONLY the attributes specified by the graph are eagerly fetched, regardless of
	 * what the mapping defaults indicate (even if they specify more properties to be loaded eagerly).
	 * 
	 * @param <T>   Root entity type of the query and graph.
	 * 
	 * @param query Query to configure.
	 * @param graph Graph of properties/attributes to eagerly load.
	 * 
	 * @return The same (input) query.
	 * 
	 * @see #setFetchGraph(Query, EntityManager, Class, CharSequence)
	 * @see #setLoadGraph(Query, EntityGraph)
	 */
	public static <T> Query setFetchGraph(final Query query, EntityGraph<T> graph) {
		return query.setHint( HINT_FETCHGRAPH, graph );
	}

	/**
	 * Configures the query hints so that ONLY the attributes specified by the graph are eagerly fetched, regardless of
	 * what the mapping defaults indicate (even if they specify more properties to be loaded eagerly).
	 * 
	 * @param <T>   Root entity type of the query and graph.
	 * 
	 * @param query Query to configure.
	 * @param graph Graph of properties/attributes to eagerly load.
	 * 
	 * @return The same (input) query.
	 * 
	 * @see #setFetchGraph(Query, EntityGraph)
	 * @see #setLoadGraph(Query, EntityGraph)
	 */
	public static <T> Query setFetchGraph(final Query query, EntityManager em, Class<T> rootType, CharSequence graph) {
		return setFetchGraph( query, EntityGraphParser.parse( em, rootType, graph ) );
	}

	/**
	 * Configures the query hints so that the attributes specified by the graph are eagerly fetched IN ADDITION to those
	 * specified to be loaded eagerly in the mapping.
	 * 
	 * @param <T>   Root entity type of the query and graph.
	 * 
	 * @param query Query to configure.
	 * @param graph Graph of properties/attributes to eagerly load.
	 * 
	 * @return The same (input) query.
	 * 
	 * @see #setFetchGraph(Query, EntityGraph)
	 * @see #setLoadGraph(Query, EntityManager, Class, CharSequence)
	 */
	public static <T> Query setLoadGraph(final Query query, EntityGraph<T> graph) {
		return query.setHint( HINT_LOADGRAPH, graph );
	}

	/**
	 * Configures the query hints so that the attributes specified by the graph are eagerly fetched IN ADDITION to those
	 * specified to be loaded eagerly in the mapping.
	 * 
	 * @param <T>   Root entity type of the query and graph.
	 * 
	 * @param query Query to configure.
	 * @param graph Graph of properties/attributes to eagerly load.
	 * 
	 * @return The same (input) query.
	 * 
	 * @see #setFetchGraph(Query, EntityGraph)
	 * @see #setLoadGraph(Query, EntityGraph)
	 */
	public static <T> Query setLoadGraph(final Query query, EntityManager em, Class<T> rootType, CharSequence graph) {
		return setLoadGraph( query, EntityGraphParser.parse( em, rootType, graph ) );
	}

	/**
	 * Creates a query just like {@link EntityManager#createQuery(String, Class)} does but allows the query string to
	 * begin with the "fetch" and/or "load" entity graph specifications (see {@link EntityGraphParser}) that will
	 * internally converted to correct hints (see {@link #setFetchGraph(Query, EntityManager, Class, CharSequence)} and
	 * {@link #setLoadGraph(Query, EntityManager, Class, CharSequence)}.
	 * 
	 * @param <T>      Root entity type of the query and graph.
	 * 
	 * @param em       EntityManager to use to create the query and entity graphs (if found).
	 * @param rootType The root type of results.
	 * @param qlString The query language string that can begin with fetch/load specification (see
	 *                 {@link EntityGraphParser}).
	 *                 
	 * @return The typed query preconfigured as per any fetch/load specifications in the {@code qlString}.
	 * 
	 * @throws InvalidGraphException if a specified fetch or load graph is invalid.
	 */
	public static <T> TypedQuery<T> createQuery(final EntityManager em, Class<T> rootType, CharSequence qlString) {
		ParseBuffer buffer = new ParseBuffer( qlString );
		FetchAndLoadEntityGraphs<T> graphs = EntityGraphParser.parsePreQueryGraphDescriptors( em, rootType, buffer );
		
		TypedQuery<T> query = em.createQuery( buffer.toString(), rootType );
		if ( graphs.fetchGraph != null ) {
			setFetchGraph( query, graphs.fetchGraph );
		}
		if ( graphs.loadGraph != null ) {
			setLoadGraph( query, graphs.loadGraph );
		}

		return query;
	}

	/**
	 * Creates a query just like {@link EntityManager#createNamedQuery(String, Class)} does but allows the query string
	 * to begin with the "fetch" and/or "load" entity graph specifications (see {@link EntityGraphParser}) that will
	 * internally converted to correct hints (see {@link #setFetchGraph(Query, EntityManager, Class, CharSequence)} and
	 * {@link #setLoadGraph(Query, EntityManager, Class, CharSequence)}.
	 * 
	 * @param <T>      Root entity type of the query and graph.
	 * 
	 * @param em       EntityManager to use to create the query and entity graphs (if found).
	 * @param rootType The root type of results.
	 * @param qlString The query language string that can begin with fetch/load specification (see
	 *                 {@link EntityGraphParser}).
	 *                 
	 * @return The typed query preconfigured as per any fetch/load specifications in the {@code qlString}.
	 * 
	 * @throws InvalidGraphException if a specified fetch or load graph is invalid.
	 */
	public static <T> TypedQuery<T> createNamedQuery(final EntityManager em, Class<T> rootType, String queryName, CharSequence graphString) {
		final EntityGraph<T> graph = EntityGraphParser.parse( em, rootType, graphString );

		final TypedQuery<T> query = em.createNamedQuery( queryName, rootType );
		setFetchGraph( query, graph );

		return query;
	}

	/**
	 * Finds an entity by its class and primary key just like
	 * {@link EntityManager#find(Class, Object, javax.persistence.LockModeType, Map)} does but includes the fetch entity
	 * graph hint as specified in the {@code graphString}.
	 * 
	 * @param <T>         Root entity type of the query and graph.
	 * 
	 * @param em          EntityManager to use.
	 * @param entityClass The type of the entity to find.
	 * @param primaryKey  The primary key of the entity to find.
	 * @param graphString The specification of attributes to fetch (see {@link EntityGraphParser}).
	 * 
	 * @return The entity if found, same as
	 * {@link EntityManager#find(Class, Object, javax.persistence.LockModeType, Map)}.
	 * 
	 * @throws InvalidGraphException if a specified fetch or load graph is invalid.
	 */
	public static <T> T find(final EntityManager em, Class<T> entityClass, Object primaryKey, CharSequence graphString) {
		em.find( entityClass, primaryKey );
		EntityGraph<T> graph = EntityGraphParser.parse( em, entityClass, graphString );

		Map<String, Object> props = new HashMap<String, Object>();
		props.put( HINT_FETCHGRAPH, graph );

		return em.find( entityClass, primaryKey, props );
	}

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
	@SafeVarargs
	public static <T> EntityGraph<T> merge(EntityManager em, Class<T> rootType, EntityGraph<T>... graphs) {
		Map<String, EntityGraphAttribute> resultMap = null;
		for ( EntityGraph<T> graph : graphs ) {
			if ( graph != null ) {
				Subgraph<T> pretendSubgraph = new GraphAsSubgraph<T>( graph, rootType );

				if ( resultMap == null ) {
					resultMap = EntityGraphAttribute.mapOf( pretendSubgraph );
				}
				else {
					EntityGraphAttribute.merge( resultMap, pretendSubgraph );
				}
			}
		}

		EntityGraph<T> merged = em.createEntityGraph( rootType );
		EntityGraphAttribute.configure( merged, rootType, resultMap );
		return merged;
	}

	/**
	 * Compares two entity graphs and returns {@code true} if they are equal, ignoring attribute order.
	 * 
	 * @param <T>  Root entity type of BOTH graphs.
	 * @param a    Graph to compare.
	 * @param b    Graph to compare.
	 * 
	 */
	public static <T> boolean equal(EntityGraph<T> a, EntityGraph<T> b) {
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
			if ( !equal( aNode, bNode ) ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Compares two entity graph attribute node and returns {@code true} if they are equal, ignoring subgraph attribute
	 * order.
	 */
	public static boolean equal(AttributeNode<?> a, AttributeNode<?> b) {
		if ( a == b ) {
			return true;
		}
		if ( ( a == null ) || ( b == null ) ) {
			return false;
		}
		if ( a.getAttributeName().equals( b.getAttributeName() ) ) {
			return equal( a.getSubgraphs(), b.getSubgraphs() ) && equal( a.getKeySubgraphs(), b.getKeySubgraphs() );
		}
		else {
			return false;
		}
	}

	/**
	 * Compares two entity subgraph maps and returns {@code true} if they are equal, ignoring order.
	 */
	public static boolean equal(@SuppressWarnings("rawtypes") Map<Class, Subgraph> a, @SuppressWarnings("rawtypes") Map<Class, Subgraph> b) {
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
				if ( !equal( a.get( clazz ), b.get( clazz ) ) ) {
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
	public static boolean equal(@SuppressWarnings("rawtypes") Subgraph a, @SuppressWarnings("rawtypes") Subgraph b) {
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
			if ( !equal( aNode, bNode ) ) {
				return false;
			}
		}

		return true;
	}
}
