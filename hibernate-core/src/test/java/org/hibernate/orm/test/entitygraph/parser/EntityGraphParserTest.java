/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A unit test of {@link GraphParser}.
 *
 * @author asusnjar
 */
public class EntityGraphParserTest extends AbstractEntityGraphTest {

	@Test
	public void testNullParsing() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( (String) null );
		Assert.assertNull( graph );
	}

	@Test
	public void testOneBasicAttributeParsing() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name" );
		AssertionHelper.assertBasicAttributes( graph, "name" );
	}

	@Test
	public void testTwoBasicAttributesParsing() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name, description" );
		AssertionHelper.assertBasicAttributes( graph, "name", "description" );
	}

	@Test
	public void testLinkParsing() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "linkToOne(name, description)" );
		assertNotNull( graph );
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );
		AttributeNode<?> node = attrs.get( 0 );
		assertNotNull( node );
		assertEquals( "linkToOne", node.getAttributeName() );
		AssertionHelper.assertNullOrEmpty( node.getKeySubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getSubgraphs();
		AssertionHelper.assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMapKeyParsing() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "map.key(name, description)" );
		assertNotNull( graph );
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );
		AttributeNode<?> node = attrs.get( 0 );
		assertNotNull( node );
		assertEquals( "map", node.getAttributeName() );
		AssertionHelper.assertNullOrEmpty( node.getSubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getKeySubgraphs();
		AssertionHelper.assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMapValueParsing() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "map.value(name, description)" );
		assertNotNull( graph );
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );
		AttributeNode<?> node = attrs.get( 0 );
		assertNotNull( node );
		assertEquals( "map", node.getAttributeName() );
		AssertionHelper.assertNullOrEmpty( node.getKeySubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getSubgraphs();
		AssertionHelper.assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMixParsingWithMaps() {
		String g = " name , linkToOne ( description, map . key ( name ) , map . value ( description ) , name ) , description , map . key ( name , description ) , map . value ( description ) ";
		g = g.replace( " ", "       " );
		for ( int i = 1; i <= 2; i++, g = g.replace( " ", "" ) ) {
			EntityGraph<GraphParsingTestEntity> graph = parseGraph( g );
			AssertionHelper.assertBasicAttributes( graph, "name", "description" );

			AttributeNode<?> linkToOne = AssertionHelper.getAttributeNodeByName( graph, "linkToOne", true );
			AssertionHelper.assertNullOrEmpty( linkToOne.getKeySubgraphs() );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneSubgraphs = linkToOne.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneRoot = linkToOneSubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( linkToOneRoot, "name", "description" );

			AttributeNode<?> linkToOneMap = AssertionHelper.getAttributeNodeByName( linkToOneRoot, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneMapKeySubgraphs = linkToOneMap.getKeySubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneMapKeyRoot = linkToOneMapKeySubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( linkToOneMapKeyRoot, "name" );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneMapSubgraphs = linkToOneMap.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneMapRoot = linkToOneMapSubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( linkToOneMapRoot, "description" );

			AttributeNode<?> map = AssertionHelper.getAttributeNodeByName( graph, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> mapKeySubgraphs = map.getKeySubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph mapKeyRoot = mapKeySubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( mapKeyRoot, "name", "description" );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> mapSubgraphs = map.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph mapRoot = mapSubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( mapRoot, "description" );
		}
	}

	@Test
	public void testMixParsingWithSimplifiedMaps() {
		String g = " name , linkToOne ( description, map . key ( name )  , name ) , description , map . value ( description, name ) ";
		g = g.replace( " ", "       " );
		for ( int i = 1; i <= 2; i++, g = g.replace( " ", "" ) ) {
			EntityGraph<GraphParsingTestEntity> graph = parseGraph( g );
			AssertionHelper.assertBasicAttributes( graph, "name", "description" );

			AttributeNode<?> linkToOne = AssertionHelper.getAttributeNodeByName( graph, "linkToOne", true );
			AssertionHelper.assertNullOrEmpty( linkToOne.getKeySubgraphs() );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneSubgraphs = linkToOne.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneRoot = linkToOneSubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( linkToOneRoot, "name", "description" );

			AttributeNode<?> linkToOneMap = AssertionHelper.getAttributeNodeByName( linkToOneRoot, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneMapKeySubgraphs = linkToOneMap.getKeySubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneMapKeyRoot = linkToOneMapKeySubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( linkToOneMapKeyRoot, "name" );

			AttributeNode<?> map = AssertionHelper.getAttributeNodeByName( graph, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> mapSubgraphs = map.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph mapRoot = mapSubgraphs.get( GraphParsingTestEntity.class );
			AssertionHelper.assertBasicAttributes( mapRoot, "description", "name" );
		}
	}

	@Test
	public void testLinkSubtypeParsing() {
		RootGraphImplementor<GraphParsingTestEntity> graph = parseGraph( "linkToOne(name, description), linkToOne(GraphParsingTestSubEntity: sub)" );
		assertNotNull( graph );

		List<? extends AttributeNodeImplementor<?,?,?>> attrs = graph.getAttributeNodeList();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );

		AttributeNodeImplementor<?,?,?> linkToOneNode = attrs.get( 0 );
		assertNotNull( linkToOneNode );
		assertEquals( "linkToOne", linkToOneNode.getAttributeName() );

		AssertionHelper.assertNullOrEmpty( linkToOneNode.getKeySubgraphs() );

		final SubGraphImplementor<?> subgraph = linkToOneNode.getSubGraphs().get( GraphParsingTestSubEntity.class );
		assertNotNull( subgraph );

		AssertionHelper.assertBasicAttributes( subgraph, "sub" );
	}

	@Test
	public void testHHH10378IsNotFixedYet() {
		EntityManager entityManager = getOrCreateEntityManager();
		RootGraphImplementor<GraphParsingTestEntity> graph = ( (SessionImplementor) entityManager ).createEntityGraph(
				GraphParsingTestEntity.class );
		final SubGraphImplementor<GraphParsingTestSubEntity> subGraph = graph.addSubGraph(
				"linkToOne",
				GraphParsingTestSubEntity.class
		);

		assertEquals( subGraph.getGraphedType().getJavaType(), GraphParsingTestSubEntity.class );

		final AttributeNodeImplementor<?,?,?> subTypeAttrNode = subGraph.findOrCreateAttributeNode( "sub" );
		assert subTypeAttrNode != null;
	}

	@Test
	public void testHHH12696MapSubgraphsKeyFirst() {

		EntityManager entityManager = getOrCreateEntityManager();
		EntityGraph<GraphParsingTestEntity> graph = entityManager.createEntityGraph( GraphParsingTestEntity.class );

		final String mapAttributeName = "map";
		Subgraph<GraphParsingTestEntity> keySubgraph = graph.addKeySubgraph( mapAttributeName );
		Subgraph<GraphParsingTestEntity> valueSubgraph = graph.addSubgraph( mapAttributeName );

		checkMapKeyAndValueSubgraphs( graph, mapAttributeName, keySubgraph, valueSubgraph );
	}

	private void checkMapKeyAndValueSubgraphs(EntityGraph<GraphParsingTestEntity> graph, final String mapAttributeName, Subgraph<GraphParsingTestEntity> keySubgraph,
			Subgraph<GraphParsingTestEntity> valueSubgraph) {
		int count = 0;
		for ( AttributeNode<?> node : graph.getAttributeNodes() ) {
			if ( mapAttributeName.equals( node.getAttributeName() ) ) {
				count++;
				@SuppressWarnings("rawtypes")
				Map<Class, Subgraph> keySubgraphs = node.getKeySubgraphs();
				Assert.assertTrue( "Missing the key subgraph", !keySubgraphs.isEmpty() );
				Assert.assertSame( keySubgraph, keySubgraphs.get( GraphParsingTestEntity.class ) );

				@SuppressWarnings("rawtypes")
				Map<Class, Subgraph> valueSubgraphs = node.getSubgraphs();
				Assert.assertTrue( "Missing the value subgraph", !valueSubgraphs.isEmpty() );
				Assert.assertSame( valueSubgraph, valueSubgraphs.get( GraphParsingTestEntity.class ) );
			}
		}
		assertEquals( 1, count );
	}

	@Test
	public void testHHH12696MapSubgraphsValueFirst() {
		EntityManager entityManager = getOrCreateEntityManager();
		EntityGraph<GraphParsingTestEntity> graph = entityManager.createEntityGraph( GraphParsingTestEntity.class );

		final String mapAttributeName = "map";
		Subgraph<GraphParsingTestEntity> valueSubgraph = graph.addSubgraph( mapAttributeName );
		Subgraph<GraphParsingTestEntity> keySubgraph = graph.addKeySubgraph( mapAttributeName );

		checkMapKeyAndValueSubgraphs( graph, mapAttributeName, keySubgraph, valueSubgraph );
	}
}
