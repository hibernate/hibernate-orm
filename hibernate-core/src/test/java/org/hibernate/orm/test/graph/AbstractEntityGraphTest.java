/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;

import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;

public abstract class AbstractEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	public AbstractEntityGraphTest() {
		super();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ GraphParsingTestEntity.class, GraphParsingTestSubentity.class };
	}

	protected <T> RootGraphImplementor<T> parseGraph(Class<T> entityType, String graphString) {
		EntityManager entityManager = getOrCreateEntityManager();
		return (RootGraphImplementor<T>) GraphParser.parse( entityType, graphString, entityManager );
	}

	protected <T> RootGraphImplementor<GraphParsingTestEntity> parseGraph(String graphString) {
		return parseGraph( GraphParsingTestEntity.class, graphString );
	}

	private <C extends Collection<?>> void assertNullOrEmpty(C collection) {
		if ( collection != null ) {
			Assert.assertEquals( 0, collection.size() );
		}
	}

	protected <M extends Map<?, ?>> void assertNullOrEmpty(M map) {
		if ( map != null ) {
			Assert.assertEquals( 0, map.size() );
		}
	}

	protected void assertBasicAttributes(EntityGraph<?> graph, String... names) {
		Assert.assertNotNull( graph );
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	protected void assertBasicAttributes(Subgraph<?> graph, String... names) {
		Assert.assertNotNull( graph );
		assertBasicAttributes( graph.getAttributeNodes(), names );
	}

	private void assertBasicAttributes(List<AttributeNode<?>> attrs, String... names) {
		if ( ( names == null ) || ( names.length == 0 ) ) {
			assertNullOrEmpty( attrs );
		}
		else {
			Assert.assertNotNull( attrs );
			Assert.assertTrue( names.length <= attrs.size() );

			for ( String name : names ) {
				AttributeNode<?> node = null;
				for ( AttributeNode<?> candidate : attrs ) {
					if ( candidate.getAttributeName().equals( name ) ) {
						node = candidate;
						break;
					}
				}
				Assert.assertNotNull( node );
				assertNullOrEmpty( node.getKeySubgraphs() );
				assertNullOrEmpty( node.getSubgraphs() );
			}
		}
	}

	protected AttributeNode<?> getAttributeNodeByName(EntityGraph<?> graph, String name, boolean required) {
		return getAttributeNodeByName( graph.getAttributeNodes(), name, required );
	}

	protected AttributeNode<?> getAttributeNodeByName(Subgraph<?> graph, String name, boolean required) {
		return getAttributeNodeByName( graph.getAttributeNodes(), name, required );
	}

	private AttributeNode<?> getAttributeNodeByName(List<AttributeNode<?>> attrs, String name, boolean required) {
		for ( AttributeNode<?> attr : attrs ) {
			if ( name.equals( attr.getAttributeName() ) )
				return attr;
		}
		if ( required )
			Assert.fail( "Required attribute not found." );
		return null;
	}

}
