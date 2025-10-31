/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.EntityGraph;
import org.hibernate.graph.EntityGraphs;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityGraphsTest extends AbstractEntityGraphTest {

	@SafeVarargs
	private <T> void checkMerge(Class<T> rootType, EntityGraph<T> expected, EntityManagerFactoryScope scope, EntityGraph<T>... graphs) {
		scope.inEntityManager(
				entityManager -> {
					EntityGraph<T> actual = EntityGraphs.merge( entityManager, rootType, graphs );
					assertThat( EntityGraphs.areEqual( expected, actual ) ).isTrue();
				}
		);
	}

	@SafeVarargs
	private void checkMerge(EntityGraph<GraphParsingTestEntity> expected, EntityManagerFactoryScope scope, EntityGraph<GraphParsingTestEntity>... graphs) {
		checkMerge( GraphParsingTestEntity.class, expected, scope, graphs );
	}

	@Test
	public void testSameBasicsEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g = parseGraph( "name, description ", scope );
		assertThat( EntityGraphs.areEqual( g, g ) ).isTrue();
	}

	@Test
	public void testEqualBasicsEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name, description ", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, name ", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testDifferentBasicsEqual1(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name, description ", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description ", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testDifferentBasicsEqual2(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name ", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description ", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testEqualLinksEqual1(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name, description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description, name)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testEqualLinksWithSubclassesEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph(
				"linkToOne(name), linkToOne(GraphParsingTestSubEntity: description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph(
				"linkToOne(GraphParsingTestSubEntity: description), linkToOne(name)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testDifferentLinksEqual1(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name, description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testDifferentLinksEqual2(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testDifferentLinksEqual3(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph(
				"linkToOne(name), linkToOne(GraphParsingTestSubEntity: description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(name, description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testEqualMapKeysEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description, name)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testDifferentMapKeysEqual1(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testDifferentMapKeysEqual2(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testEqualMapValuesEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description, name)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testDifferentMapValuesEqual1(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testDifferentMapValuesEqual2(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testEqualComplexGraphsEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph(
				"map.key(name, description), name, linkToOne(description), description", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph(
				"description, map.key(description, name), name, linkToOne(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testDifferentComplexGraphsEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph(
				"map.key(name, description), name, linkToOne(description), description", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph(
				"description, map.value(description, name), name, linkToOne(description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}

	@Test
	public void testNullsEqual() {
		assertThat( EntityGraphs.areEqual( null, (EntityGraph<GraphParsingTestEntity>) null ) ).isTrue();
	}

	@Test
	public void testNullAndNonNullEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name ", scope );
		assertThat( EntityGraphs.areEqual( graph, null ) ).isFalse();
		assertThat( EntityGraphs.areEqual( null, graph ) ).isFalse();
	}

	@Test
	public void testBasicMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "name", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "description", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "name, description ", scope );
		checkMerge( expected, scope, g1, g2 );
	}

	@Test
	public void testLinkMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "linkToOne(name)", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "linkToOne(description)", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "linkToOne(name, description) ", scope );
		checkMerge( expected, scope, g1, g2 );
	}

	@Test
	public void testMapKeyMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "map.key(name)", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "map.key(description)", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "map.key(name, description) ", scope );
		checkMerge( expected, scope, g1, g2 );
	}

	@Test
	public void testMapValueMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "map.value(name)", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "map.value(description)", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "map.value(name, description) ", scope );
		checkMerge( expected, scope, g1, g2 );
	}
}
