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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

import org.hibernate.graph.EntityGraphParser;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;

public abstract class AbstractEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	public AbstractEntityGraphTest() {
		super();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ GraphParsingTestEntity.class, GraphParsingTestSubentity.class };
	}

	protected <T> EntityGraph<T> parseGraph(Class<T> entityType, String graphString) {
		EntityManager entityManager = getOrCreateEntityManager();
		return EntityGraphParser.parse( entityManager, entityType, graphString );
	}

	protected <T> EntityGraph<GraphParsingTestEntity> parseGraph(String graphString) {
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
