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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

/**
 * A temporary internal representation of an entity graph attribute used in the process of
 * {@linkplain EntityGraphs#merge(EntityManager, Class, EntityGraph...) merging multiple entity graphs}
 * together.
 * 
 * @author asusnjar
 */
class EntityGraphAttribute {

	/**
	 * Name of the attribute.
	 */
	final String name;
	
	/**
	 * An attributeSubclass-&gt;attributeName-&gt;{@code EntityGraphAttribute} map 
	 * for map attribute keys (only applies to map attributes).
	 */
	Map<Class<?>, Map<String, EntityGraphAttribute>> keySubgraphs = 
			new HashMap<Class<?>, Map<String, EntityGraphAttribute>>();
	
	/**
	 * An attributeSubclass-&gt;attributeName-&gt;{@code EntityGraphAttribute} map 
	 * for collection and/or map attribute values.
	 */
	Map<Class<?>, Map<String, EntityGraphAttribute>> valueSubgraphs = 
			new HashMap<Class<?>, Map<String, EntityGraphAttribute>>();

	/**
	 * Basic attribute constructor.
	 * 
	 * @param name Name of the attribute.
	 */
	EntityGraphAttribute(String name) {
		this.name = name;
	}

	/**
	 * Constructor from a JPA {@link AttributeNode}.
	 * 
	 * @param attrNode JPA attribute node to replicate.
	 */
	EntityGraphAttribute(AttributeNode<?> attrNode) {
		this( attrNode.getAttributeName() );

		copySubgraphs( keySubgraphs, attrNode.getKeySubgraphs() );
		copySubgraphs( valueSubgraphs, attrNode.getSubgraphs() );
	}

	/**
	 * Copies a JPA class-&gt;subgraph map to an internal
	 * class-&gt;attributeName-&gt;{@code EntityGraphAttribute} map structure.
	 * 
	 * @param dest    Internal map to replicate the source subgraphs into.
	 * @param source  JPA class-subgraph map to copy subgraphs from.
	 */
	@SuppressWarnings("rawtypes")
	private static void copySubgraphs(Map<Class<?>, Map<String, EntityGraphAttribute>> dest, Map<Class, Subgraph> source) {
		for ( Map.Entry<Class, Subgraph> entry : source.entrySet() ) {
			dest.put( entry.getKey(), mapOf( entry.getValue() ) );
		}
	}

	/**
	 * Converts a JPA Subgraph to an internally used attributeName-&gt;{@code EntityGraphAttribute} map.
	 * 
	 * @param jpaSubgraph JPA subgraph to convert.
	 */
	static Map<String, EntityGraphAttribute> mapOf(Subgraph<?> jpaSubgraph) {
		Map<String, EntityGraphAttribute> result = null;
		if ( jpaSubgraph != null ) {
			result = new HashMap<String, EntityGraphAttribute>();

			for ( AttributeNode<?> node : jpaSubgraph.getAttributeNodes() ) {
				result.put( node.getAttributeName(), new EntityGraphAttribute( node ) );
			}
		}

		return result;
	}

	/**
	 * Merges the {@code source} attributeName-&gt;{@link EntityGraphAttribute} into {@code dest}. 
	 * 
	 * @param dest Map of attributes to merge {@code source} into.
	 * 
	 * @param source Map of attributes to merge into {@code dest}.
	 * 
	 * @see #merge(Map, Subgraph)
	 */
	static void merge(Map<String, EntityGraphAttribute> dest, Map<String, EntityGraphAttribute> source) {
		for ( Map.Entry<String, EntityGraphAttribute> entry : source.entrySet() ) {
			String name = entry.getKey();
			EntityGraphAttribute sourceAttr = entry.getValue();
			EntityGraphAttribute destAttr = dest.get( name );

			if ( destAttr == null ) {
				dest.put( name, sourceAttr );
			}
			else {
				mergeSubgraphMaps( destAttr.keySubgraphs, sourceAttr.keySubgraphs );
				mergeSubgraphMaps( destAttr.valueSubgraphs, sourceAttr.valueSubgraphs );
			}
		}
	}

	/**
	 * Merges the {@code source} subgraph attributes into the {@code dest} attributeName-&gt;{@link EntityGraphAttribute} map. 
	 * 
	 * @param dest Map of attributes to merge {@code source} into.
	 * 
	 * @param source Subgraph to merge into {@code dest}.
	 * 
	 * @see #merge(Map, Map)
	 */
	static void merge(Map<String, EntityGraphAttribute> dest, Subgraph<?> source) {
		merge( dest, mapOf( source ) );
	}

	/**
	 * Merges the {@code source} class-&gt;name-&gt;{@link EntityGraphAttribute} map into {@code dest}. 
	 * 
	 * @param dest Map of attributes to merge {@code source} into.
	 * 
	 * @param source Map of attributes to merge into {@code dest}.
	 */
	private static void mergeSubgraphMaps(Map<Class<?>, Map<String, EntityGraphAttribute>> dest, Map<Class<?>, Map<String, EntityGraphAttribute>> source) {
		for ( Map.Entry<Class<?>, Map<String, EntityGraphAttribute>> entry : source.entrySet() ) {
			Class<?> subclass = entry.getKey();
			Map<String, EntityGraphAttribute> sourceMap = entry.getValue();
			Map<String, EntityGraphAttribute> destMap = dest.get( subclass );

			if ( destMap == null ) {
				dest.put( subclass, sourceMap );
			}
			else {
				merge( destMap, sourceMap );
			}
		}
	}

	/**
	 * Configures the specified entity graph given a root type and attributeName-&gt;{@code EntityGraphAttribute} map.
	 * 
	 * @param <T>       Root entity type of the graph.
	 * @param graph     Graph to configure.
	 * @param rootType  Root entity type of the graph.
	 * @param map       An attributeName-&gt;{@code EntityGraphAttribute} map to apply to the graph.
	 * 
	 * @see #configure(Subgraph, Map)
	 */
	static <T> void configure(EntityGraph<T> graph, Class<T> rootType, Map<String, EntityGraphAttribute> map) {
		configure( new GraphAsSubgraph<T>( graph, rootType ), map );
	}

	/**
	 * Configures the specified entity subgraph given an attributeName-&gt;{@code EntityGraphAttribute} map.
	 * 
	 * @param subgraph  Subraph to configure.
	 * @param map       An attributeName-&gt;{@code EntityGraphAttribute} map to apply to the subgraph.
	 * 
	 * @see #configure(EntityGraph, Class, Map)
	 */
	static void configure(Subgraph<?> subgraph, Map<String, EntityGraphAttribute> map) {
		for ( EntityGraphAttribute attr : map.values() ) {
			attr.configure( subgraph );
		}
	}

	/**
	 * Applies the information in 'this' {@code EntityGraphAttribute} to configure the specified subgraph.
	 * 
	 * @param subgraph JPA subgraph to configure.
	 */
	private void configure(Subgraph<?> subgraph) {
		if ( !keySubgraphs.isEmpty() ) {
			for ( Map.Entry<Class<?>, Map<String, EntityGraphAttribute>> entry : keySubgraphs.entrySet() ) {
				Class<?> subclass = entry.getKey();

				Subgraph<?> innerSubgraph;

				if ( subclass == null ) {
					innerSubgraph = subgraph.addKeySubgraph( name );
				}
				else {
					innerSubgraph = subgraph.addKeySubgraph( name, subclass );
				}

				configure( innerSubgraph, entry.getValue() );
			}
		}

		if ( !valueSubgraphs.isEmpty() ) {
			for ( Map.Entry<Class<?>, Map<String, EntityGraphAttribute>> entry : valueSubgraphs.entrySet() ) {
				Class<?> subclass = entry.getKey();

				Subgraph<?> innerSubgraph;

				if ( subclass == null ) {
					innerSubgraph = subgraph.addSubgraph( name );
				}
				else {
					innerSubgraph = subgraph.addSubgraph( name, subclass );
				}

				configure( innerSubgraph, entry.getValue() );
			}
		}

		if ( keySubgraphs.isEmpty() && valueSubgraphs.isEmpty() ) {
			subgraph.addAttributeNodes( name );
		}
	}
}
