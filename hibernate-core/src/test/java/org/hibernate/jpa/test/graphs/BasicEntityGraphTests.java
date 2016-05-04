/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
