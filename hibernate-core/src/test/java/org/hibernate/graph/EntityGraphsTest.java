/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EntityGraphsTest extends AbstractEntityGraphTest {

	private final <T> void checkMerge(Class<T> rootType, EntityGraph<T> expected, @SuppressWarnings("unchecked") EntityGraph<T>... graphs) {
		EntityManager entityManager = getOrCreateEntityManager();
		EntityGraph<T> actual = EntityGraphs.merge( entityManager, rootType, graphs );
		Assert.assertTrue( EntityGraphs.areEqual( expected, actual ) );
	}

	@SafeVarargs
	private final void checkMerge(EntityGraph<GraphParsingTestEntity> expected, EntityGraph<GraphParsingTestEntity>... graphs) {
		checkMerge( GraphParsingTestEntity.class, expected, graphs );
	}

	@Test
	public void testSameBasicsEqual() {
		EntityGraph<GraphParsingTestEntity> g = parseGraph( "name, description " );
		Assert.assertTrue( EntityGraphs.areEqual( g, g ) );
	}

	@Test
	public void testEqualBasicsEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name, description " );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, name " );
		Assert.assertTrue( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentBasicsEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name, description " );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description " );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentBasicsEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name " );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description " );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testEqualLinksEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description, name)" );
		Assert.assertTrue( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	@Ignore("Cannot run due to Hibernate bug: https://hibernate.atlassian.net/browse/HHH-10378")
	public void testEqualLinksWithSubclassesEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name), linkToOne:MockSubentity(description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne:MockSubentity(description), linkToOne(name)" );
		Assert.assertTrue( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentLinksEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentLinksEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	@Ignore("Cannot run due to Hibernate bug: https://hibernate.atlassian.net/browse/HHH-10378")
	public void testDifferentLinksEqual3() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name), linkToOne:MockSubentity(description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(name, description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testEqualMapKeysEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description, name)" );
		Assert.assertTrue( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentMapKeysEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentMapKeysEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testEqualMapValuesEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description, name)" );
		Assert.assertTrue( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentMapValuesEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentMapValuesEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testEqualComplexGraphsEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description), name, linkToOne(description), description" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, map.key(description, name), name, linkToOne(description)" );
		Assert.assertTrue( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testDifferentComplexGraphsEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description), name, linkToOne(description), description" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, map.value(description, name), name, linkToOne(description)" );
		Assert.assertFalse( EntityGraphs.areEqual( a, b ) );
	}

	@Test
	public void testNullsEqual() {
		Assert.assertTrue( EntityGraphs.areEqual( (EntityGraph<GraphParsingTestEntity>) null, (EntityGraph<GraphParsingTestEntity>) null ) );
	}

	@Test
	public void testNullAndNonNullEqual() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name " );
		Assert.assertFalse( EntityGraphs.areEqual( graph, (EntityGraph<GraphParsingTestEntity>) null ) );
		Assert.assertFalse( EntityGraphs.areEqual( (EntityGraph<GraphParsingTestEntity>) null, graph ) );
	}

	@Test
	public void testBasicMerge() {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "name" );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "description" );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "name, description " );
		checkMerge( expected, g1, g2 );
	}

	@Test
	public void testLinkMerge() {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "linkToOne(name)" );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "linkToOne(description)" );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "linkToOne(name, description) " );
		checkMerge( expected, g1, g2 );
	}

	@Test
	public void testMapKeyMerge() {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "map.key(name)" );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "map.key(description)" );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "map.key(name, description) " );
		checkMerge( expected, g1, g2 );
	}

	@Test
	public void testMapValueMerge() {
		EntityGraph<GraphParsingTestEntity> g1 = parseGraph( "map.value(name)" );
		EntityGraph<GraphParsingTestEntity> g2 = parseGraph( "map.value(description)" );
		EntityGraph<GraphParsingTestEntity> expected = parseGraph( "map.value(name, description) " );
		checkMerge( expected, g1, g2 );
	}
}
