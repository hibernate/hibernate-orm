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
package org.hibernate.jpa.test.graph;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.graph.EntityGraphs;

public class EntityGraphsTest extends AbstractEntityGraphTest {

	private final <T> void checkMerge(Class<T> rootType, EntityGraph<T> expected, @SuppressWarnings("unchecked") EntityGraph<T>... graphs) {
		EntityManager entityManager = getOrCreateEntityManager();
		EntityGraph<T> actual = EntityGraphs.merge( entityManager, rootType, graphs );
		Assert.assertTrue( EntityGraphs.equal( expected, actual ) );
	}

	@SafeVarargs
	private final void checkMerge(EntityGraph<GraphParsingTestEntity> expected, EntityGraph<GraphParsingTestEntity>... graphs) {
		checkMerge( GraphParsingTestEntity.class, expected, graphs );
	}

	@Test
	public void testSameBasicsEqual() {
		EntityGraph<GraphParsingTestEntity> g = parseGraph( "name, description " );
		Assert.assertTrue( EntityGraphs.equal( g, g ) );
	}

	@Test
	public void testEqualBasicsEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name, description " );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, name " );
		Assert.assertTrue( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentBasicsEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name, description " );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description " );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentBasicsEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "name " );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description " );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testEqualLinksEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description, name)" );
		Assert.assertTrue( EntityGraphs.equal( a, b ) );
	}

	@Test
	@Ignore("Cannot run due to Hibernate bug: https://hibernate.atlassian.net/browse/HHH-10378")
	public void testEqualLinksWithSubclassesEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name), linkToOne:MockSubentity(description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne:MockSubentity(description), linkToOne(name)" );
		Assert.assertTrue( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentLinksEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentLinksEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	@Ignore("Cannot run due to Hibernate bug: https://hibernate.atlassian.net/browse/HHH-10378")
	public void testDifferentLinksEqual3() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "linkToOne(name), linkToOne:MockSubentity(description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(name, description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testEqualMapKeysEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description, name)" );
		Assert.assertTrue( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentMapKeysEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentMapKeysEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.key(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testEqualMapValuesEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description, name)" );
		Assert.assertTrue( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentMapValuesEqual1() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name, description)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentMapValuesEqual2() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.value(name)" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "map.value(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testEqualComplexGraphsEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description), name, linkToOne(description), description" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, map.key(description, name), name, linkToOne(description)" );
		Assert.assertTrue( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testDifferentComplexGraphsEqual() {
		EntityGraph<GraphParsingTestEntity> a = parseGraph( "map.key(name, description), name, linkToOne(description), description" );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "description, map.value(description, name), name, linkToOne(description)" );
		Assert.assertFalse( EntityGraphs.equal( a, b ) );
	}

	@Test
	public void testNullsEqual() {
		Assert.assertTrue( EntityGraphs.equal( (EntityGraph<GraphParsingTestEntity>) null, (EntityGraph<GraphParsingTestEntity>) null ) );
	}

	@Test
	public void testNullAndNonNullEqual() {
		EntityGraph<GraphParsingTestEntity> graph = parseGraph( "name " );
		Assert.assertFalse( EntityGraphs.equal( graph, (EntityGraph<GraphParsingTestEntity>) null ) );
		Assert.assertFalse( EntityGraphs.equal( (EntityGraph<GraphParsingTestEntity>) null, graph ) );
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
