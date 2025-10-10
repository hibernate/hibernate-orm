/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * A unit test of {@link GraphParser}.
 *
 * @author asusnjar
 */
public class EntityGraphParserTest extends AbstractEntityGraphTest {

	@Test
	public void testNullParsing(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( null, scope );
		assertThat( graph ).isNull();
	}

	@Test
	public void testOneBasicAttributeParsing(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name", scope );
		AssertionHelper.assertBasicAttributes( graph, "name" );
	}

	@Test
	public void testTwoBasicAttributesParsing(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name, description", scope );
		AssertionHelper.assertBasicAttributes( graph, "name", "description" );
	}

	@Test
	public void testLinkParsing(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "linkToOne(name, description)", scope );
		assertThat( graph ).isNotNull();

		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertThat( attrs ).isNotNull();

		assertThat( attrs.size() ).isEqualTo( 1 );
		AttributeNode<?> node = attrs.get( 0 );
		assertThat( node ).isNotNull();
		assertThat( node.getAttributeName() ).isEqualTo( "linkToOne" );
		AssertionHelper.assertNullOrEmpty( node.getKeySubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getSubgraphs();
		AssertionHelper.assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMapKeyParsing(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "map.key(name, description)", scope );
		assertThat( graph ).isNotNull();
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertThat( attrs ).isNotNull();
		assertThat( attrs.size() ).isEqualTo( 1 );
		AttributeNode<?> node = attrs.get( 0 );
		assertThat( node ).isNotNull();
		assertThat( node.getAttributeName() ).isEqualTo( "map" );
		AssertionHelper.assertNullOrEmpty( node.getSubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getKeySubgraphs();
		AssertionHelper.assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMapValueParsing(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "map.value(name, description)", scope );
		assertThat( graph ).isNotNull();
		List<AttributeNode<?>> attrs = graph.getAttributeNodes();
		assertThat( attrs ).isNotNull();
		assertThat( attrs.size() ).isEqualTo( 1 );
		AttributeNode<?> node = attrs.get( 0 );
		assertThat( node ).isNotNull();
		assertThat( node.getAttributeName() ).isEqualTo( "map" );
		AssertionHelper.assertNullOrEmpty( node.getKeySubgraphs() );
		@SuppressWarnings("rawtypes")
		Map<Class, Subgraph> sub = node.getSubgraphs();
		AssertionHelper.assertBasicAttributes( sub.get( GraphParsingTestEntity.class ), "name", "description" );
	}

	@Test
	public void testMixParsingWithMaps(EntityManagerFactoryScope scope) {
		String g = " name , linkToOne ( description, map . key ( name ) , map . value ( description ) , name ) , description , map . key ( name , description ) , map . value ( description ) ";
		g = g.replace( " ", "       " );
		for ( int i = 1; i <= 2; i++, g = g.replace( " ", "" ) ) {
			EntityGraph<GraphParsingTestEntity> graph = parseGraph( g, scope );
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
	public void testMixParsingWithSimplifiedMaps(EntityManagerFactoryScope scope) {
		String g = " name , linkToOne ( description, map . key ( name )  , name ) , description , map . value ( description, name ) ";
		g = g.replace( " ", "       " );
		for ( int i = 1; i <= 2; i++, g = g.replace( " ", "" ) ) {
			EntityGraph<GraphParsingTestEntity> graph = parseGraph( g, scope );
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
	public void testLinkSubtypeParsing(EntityManagerFactoryScope scope) {
		RootGraphImplementor<GraphParsingTestEntity> graph = parseGraph(
				"linkToOne(name, description), linkToOne(GraphParsingTestSubEntity: sub)", scope );
		assertThat( graph ).isNotNull();

		List<? extends AttributeNodeImplementor<?, ?, ?>> attrs = graph.getAttributeNodeList();
		assertThat( attrs ).isNotNull();
		assertThat( attrs.size() ).isEqualTo( 1 );

		AttributeNodeImplementor<?, ?, ?> linkToOneNode = attrs.get( 0 );
		assertThat( linkToOneNode ).isNotNull();
		assertThat( linkToOneNode.getAttributeName() ).isEqualTo( "linkToOne" );

		AssertionHelper.assertNullOrEmpty( linkToOneNode.getKeySubgraphs() );

		final SubGraphImplementor<?> subgraph = linkToOneNode.getSubGraphs().get( GraphParsingTestSubEntity.class );
		assertThat( subgraph ).isNotNull();

		AssertionHelper.assertBasicAttributes( subgraph, "sub" );
	}

	@Test
	public void testHHH10378IsNotFixedYet(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					RootGraphImplementor<GraphParsingTestEntity> graph = ((SessionImplementor) entityManager)
							.createEntityGraph( GraphParsingTestEntity.class );
					final SubGraphImplementor<GraphParsingTestSubEntity> subGraph = graph.addSubGraph(
							"linkToOne",
							GraphParsingTestSubEntity.class
					);

					assertThat( GraphParsingTestSubEntity.class ).isEqualTo( subGraph.getGraphedType().getJavaType() );

					final AttributeNodeImplementor<?, ?, ?> subTypeAttrNode = subGraph.findOrCreateAttributeNode(
							"sub" );
					assert subTypeAttrNode != null;
				}
		);

	}

	@Test
	public void testHHH12696MapSubgraphsKeyFirst(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					EntityGraph<GraphParsingTestEntity> graph = entityManager
							.createEntityGraph( GraphParsingTestEntity.class );

					final String mapAttributeName = "map";
					Subgraph<GraphParsingTestEntity> keySubgraph = graph.addKeySubgraph( mapAttributeName );
					Subgraph<GraphParsingTestEntity> valueSubgraph = graph.addSubgraph( mapAttributeName );

					checkMapKeyAndValueSubgraphs( graph, mapAttributeName, keySubgraph, valueSubgraph );
				}
		);
	}

	private void checkMapKeyAndValueSubgraphs(
			EntityGraph<GraphParsingTestEntity> graph,
			final String mapAttributeName,
			Subgraph<GraphParsingTestEntity> keySubgraph,
			Subgraph<GraphParsingTestEntity> valueSubgraph) {
		int count = 0;
		for ( AttributeNode<?> node : graph.getAttributeNodes() ) {
			if ( mapAttributeName.equals( node.getAttributeName() ) ) {
				count++;
				@SuppressWarnings("rawtypes")
				Map<Class, Subgraph> keySubgraphs = node.getKeySubgraphs();
				assertThat( !keySubgraphs.isEmpty() )
						.describedAs( "Missing the key subgraph" )
						.isTrue();
				assertThat( keySubgraphs.get( GraphParsingTestEntity.class ) ).isSameAs( keySubgraph );

				@SuppressWarnings("rawtypes")
				Map<Class, Subgraph> valueSubgraphs = node.getSubgraphs();
				assertThat( !valueSubgraphs.isEmpty() )
						.describedAs( "Missing the value subgraph" )
						.isTrue();
				assertThat( valueSubgraphs.get( GraphParsingTestEntity.class ) ).isSameAs( valueSubgraph );
			}
		}
		assertThat( count ).isEqualTo( 1 );
	}

	@Test
	public void testHHH12696MapSubgraphsValueFirst(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					EntityGraph<GraphParsingTestEntity> graph = entityManager
							.createEntityGraph( GraphParsingTestEntity.class );

					final String mapAttributeName = "map";
					Subgraph<GraphParsingTestEntity> valueSubgraph = graph.addSubgraph( mapAttributeName );
					Subgraph<GraphParsingTestEntity> keySubgraph = graph.addKeySubgraph( mapAttributeName );

					checkMapKeyAndValueSubgraphs( graph, mapAttributeName, keySubgraph, valueSubgraph );
				}
		);
	}
}
