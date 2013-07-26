/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.graphs;

import javax.persistence.AttributeNode;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Subgraph;

import java.util.Set;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class BasicEntityGraphTests extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity1.class };
	}

	@Test
	public void testBasicGraphBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph<Entity1> graphRoot = em.createEntityGraph( Entity1.class );
		assertNull( graphRoot.getName() );
		assertEquals( 0, graphRoot.getAttributeNodes().size() );
	}

	@Test
	public void testBasicSubgraphBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph<Entity1> graphRoot = em.createEntityGraph( Entity1.class );
		Subgraph<Entity1> parentGraph = graphRoot.addSubgraph( "parent" );
		Subgraph<Entity1> childGraph = graphRoot.addSubgraph( "children" );

		assertNull( graphRoot.getName() );
		assertEquals( 2, graphRoot.getAttributeNodes().size() );
		assertTrue(
				graphRoot.getAttributeNodes().get( 0 ).getSubgraphs().containsValue( parentGraph )
						|| graphRoot.getAttributeNodes().get( 0 ).getSubgraphs().containsValue( childGraph )
		);
		assertTrue(
				graphRoot.getAttributeNodes().get( 1 ).getSubgraphs().containsValue( parentGraph )
						|| graphRoot.getAttributeNodes().get( 1 ).getSubgraphs().containsValue( childGraph )
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBasicGraphImmutability() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph<Entity1> graphRoot = em.createEntityGraph( Entity1.class );
		graphRoot.addSubgraph( "parent" );
		graphRoot.addSubgraph( "children" );

		em.getEntityManagerFactory().addNamedEntityGraph( "immutable", graphRoot );

		graphRoot = (EntityGraph<Entity1>) em.getEntityGraph( "immutable" );

		assertEquals( "immutable", graphRoot.getName() );
		assertEquals( 2, graphRoot.getAttributeNodes().size() );
		try {
			graphRoot.addAttributeNodes( "parent" );
			fail( "Should have failed" );
		}
		catch (IllegalStateException ignore) {
			// expected outcome
		}

		for ( AttributeNode attrNode : graphRoot.getAttributeNodes() ) {
			assertEquals( 1, attrNode.getSubgraphs().size() );
			Subgraph subgraph = (Subgraph) attrNode.getSubgraphs().values().iterator().next();
			try {
				graphRoot.addAttributeNodes( "parent" );
				fail( "Should have failed" );
			}
			catch (IllegalStateException ignore) {
				// expected outcome
			}
		}
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		public Integer id;
		public String name;
		@ManyToOne
		public Entity1 parent;
		@OneToMany( mappedBy = "parent" )
		public Set<Entity1> children;
	}
}
