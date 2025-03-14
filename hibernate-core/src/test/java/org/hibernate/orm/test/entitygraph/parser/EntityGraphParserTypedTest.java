/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hibernate.orm.test.entitygraph.parser.AssertionHelper.assertBasicAttributes;
import static org.hibernate.orm.test.entitygraph.parser.AssertionHelper.assertNullOrEmpty;
import static org.hibernate.orm.test.entitygraph.parser.AssertionHelper.getAttributeNodeByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A unit test of {@link GraphParser} using root entity name in the graph text.
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {GraphParsingTestEntity.class, GraphParsingTestSubEntity.class })
@SessionFactory(exportSchema = false)
public class EntityGraphParserTypedTest {

	@Test
	public void testOneBasicAttributeParsing(SessionFactoryScope factoryScope) {
		final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
				"GraphParsingTestEntity: name",
				factoryScope.getSessionFactory()
		);
		assertBasicAttributes( graph, "name" );
	}

	@Test
	public void testTwoBasicAttributesParsing(SessionFactoryScope factoryScope) {
		final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
				"GraphParsingTestEntity: name, description",
				factoryScope.getSessionFactory()
		);
		assertBasicAttributes( graph, "name", "description" );
	}

	@Test
	public void testLinkParsing(SessionFactoryScope factoryScope) {
		final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
				"GraphParsingTestEntity: linkToOne(name, description)",
				factoryScope.getSessionFactory()
		);
		assertNotNull( graph );
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );
		AttributeNode<?> node = attrs.get( 0 );
		assertNotNull( node );
		assertEquals( "linkToOne", node.getAttributeName() );
		assertNullOrEmpty( node.getKeySubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getSubgraphs();
		assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMapKeyParsing(SessionFactoryScope factoryScope) {
		final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
				"GraphParsingTestEntity: map.key(name, description)",
				factoryScope.getSessionFactory()
		);
		assertNotNull( graph );
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );
		AttributeNode<?> node = attrs.get( 0 );
		assertNotNull( node );
		assertEquals( "map", node.getAttributeName() );
		assertNullOrEmpty( node.getSubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getKeySubgraphs();
		assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMapValueParsing(SessionFactoryScope factoryScope) {
		final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
				"GraphParsingTestEntity: map.value(name, description)",
				factoryScope.getSessionFactory()
		);
		assertNotNull( graph );
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );
		AttributeNode<?> node = attrs.get( 0 );
		assertNotNull( node );
		assertEquals( "map", node.getAttributeName() );
		assertNullOrEmpty( node.getKeySubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getSubgraphs();
		assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMixParsingWithMaps(SessionFactoryScope factoryScope) {
		String g = " name , linkToOne ( description, map . key ( name ) , map . value ( description ) , name ) , description , map . key ( name , description ) , map . value ( description ) ";
		g = g.replace( " ", "       " );
		for ( int i = 1; i <= 2; i++, g = g.replace( " ", "" ) ) {
			final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
					"GraphParsingTestEntity: " + g,
					factoryScope.getSessionFactory()
			);
			assertBasicAttributes( graph, "name", "description" );

			AttributeNode<?> linkToOne = getAttributeNodeByName( graph, "linkToOne", true );
			assertNullOrEmpty( linkToOne.getKeySubgraphs() );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneSubgraphs = linkToOne.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneRoot = linkToOneSubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( linkToOneRoot, "name", "description" );

			AttributeNode<?> linkToOneMap = getAttributeNodeByName( linkToOneRoot, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneMapKeySubgraphs = linkToOneMap.getKeySubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneMapKeyRoot = linkToOneMapKeySubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( linkToOneMapKeyRoot, "name" );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneMapSubgraphs = linkToOneMap.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneMapRoot = linkToOneMapSubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( linkToOneMapRoot, "description" );

			AttributeNode<?> map = getAttributeNodeByName( graph, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> mapKeySubgraphs = map.getKeySubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph mapKeyRoot = mapKeySubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( mapKeyRoot, "name", "description" );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> mapSubgraphs = map.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph mapRoot = mapSubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( mapRoot, "description" );
		}
	}

	@Test
	public void testMixParsingWithSimplifiedMaps(SessionFactoryScope factoryScope) {
		String g = " name , linkToOne ( description, map . key ( name )  , name ) , description , map . value ( description, name ) ";
		g = g.replace( " ", "       " );
		for ( int i = 1; i <= 2; i++, g = g.replace( " ", "" ) ) {
			final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
					"GraphParsingTestEntity: " + g,
					factoryScope.getSessionFactory()
			);
			assertBasicAttributes( graph, "name", "description" );

			AttributeNode<?> linkToOne = getAttributeNodeByName( graph, "linkToOne", true );
			assertNullOrEmpty( linkToOne.getKeySubgraphs() );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneSubgraphs = linkToOne.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneRoot = linkToOneSubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( linkToOneRoot, "name", "description" );

			AttributeNode<?> linkToOneMap = getAttributeNodeByName( linkToOneRoot, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> linkToOneMapKeySubgraphs = linkToOneMap.getKeySubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph linkToOneMapKeyRoot = linkToOneMapKeySubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( linkToOneMapKeyRoot, "name" );

			AttributeNode<?> map = getAttributeNodeByName( graph, "map", true );
			@SuppressWarnings("rawtypes")
			Map<Class, Subgraph> mapSubgraphs = map.getSubgraphs();
			@SuppressWarnings("rawtypes")
			Subgraph mapRoot = mapSubgraphs.get( GraphParsingTestEntity.class );
			assertBasicAttributes( mapRoot, "description", "name" );
		}
	}

	@Test
	public void testLinkSubtypeParsing(SessionFactoryScope factoryScope) {
		final EntityGraph<GraphParsingTestEntity> graph = GraphParser.parse(
				"GraphParsingTestEntity: linkToOne(name, description), linkToOne(GraphParsingTestSubEntity: sub)",
				factoryScope.getSessionFactory()
		);
		assertNotNull( graph );

		List<? extends AttributeNodeImplementor<?,?,?>> attrs = ( (RootGraphImplementor) graph ).getAttributeNodeList();
		assertNotNull( attrs );
		assertEquals( 1, attrs.size() );

		AttributeNodeImplementor<?,?,?> linkToOneNode = attrs.get( 0 );
		assertNotNull( linkToOneNode );
		assertEquals( "linkToOne", linkToOneNode.getAttributeName() );

		assertNullOrEmpty( linkToOneNode.getKeySubgraphs() );

		final SubGraphImplementor<?> subgraph = linkToOneNode.getSubGraphs().get( GraphParsingTestSubEntity.class );
		assertNotNull( subgraph );

		assertBasicAttributes( subgraph, "sub" );
	}
}
