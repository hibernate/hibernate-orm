/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.graph;

import jakarta.persistence.EntityGraph;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.EntityGraphs;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityGraphsTest extends AbstractEntityGraphTest {

	private <T> void checkMerge(EntityManagerFactoryScope scope, Class<T> rootType, EntityGraph<T> expected, EntityGraph<T>... graphs) {
		scope.inEntityManager(
				entityManager -> {
					EntityGraph<T> actual = EntityGraphs.merge( entityManager, rootType, graphs );
					assertThat( EntityGraphs.areEqual( expected, actual ) ).isTrue();
				}
		);
	}

	@SafeVarargs
	private void checkMerge(EntityManagerFactoryScope scope, EntityGraph<GraphParsingTestEntity> expected, EntityGraph<GraphParsingTestEntity>... graphs) {
		checkMerge( scope, GraphParsingTestEntity.class, expected, graphs );
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
				"linkToOne(name), linkToOne(GraphParsingTestSubentity:description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph(
				"linkToOne(GraphParsingTestSubentity:description), linkToOne(name)", scope );
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
				"linkToOne(name), linkToOne(GraphParsingTestSubentity:description)", scope );
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
	public void testDifferentMapValuesEqual1(EntityManagerFactoryScope scop) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)", scop );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)", scop );
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
	public void testNullsEqual(EntityManagerFactoryScope scope) {
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
		checkMerge( scope, expected, g1, g2 );
	}

	@Test
	public void testLinkMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "linkToOne(name)", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "linkToOne(description)", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "linkToOne(name, description) ", scope );
		checkMerge( scope, expected, g1, g2 );
	}

	@Test
	public void testMapKeyMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "map.key(name)", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "map.key(description)", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "map.key(name, description) ", scope );
		checkMerge( scope, expected, g1, g2 );
	}

	@Test
	public void testMapValueMerge(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "map.value(name)", scope );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "map.value(description)", scope );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "map.value(name, description) ", scope );
		checkMerge( scope, expected, g1, g2 );
	}

	@Test
	@JiraKey(value = "HHH-14264")
	public void testRootGraphAppliesToChildEntityClass(EntityManagerFactoryScope scope) {
		RootGraphImplementor<GraphParsingTestEntity> rootGraphImplementor = parseGraph( GraphParsingTestEntity.class,
				"name, description", scope );
		EntityDomainType<?> entity = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
				.getJpaMetamodel()
				.entity( (Class<?>) GraphParsingTestSubentity.class );
		assertThat( rootGraphImplementor.appliesTo( entity ) ).isTrue();
	}
}
