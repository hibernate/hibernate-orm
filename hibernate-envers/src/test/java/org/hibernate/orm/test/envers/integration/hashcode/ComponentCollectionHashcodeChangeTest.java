/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.hashcode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11063")
public class ComponentCollectionHashcodeChangeTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ComponentEntity.class, Component.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			// Revision 1 - Create entity with 2 components
			entityManager.getTransaction().begin();
			Component component1 = new Component();
			component1.setName( "User1" );
			component1.setData( "Test1" );
			Component component2 = new Component();
			component2.setName( "User2" );
			component2.setData( "Test2" );
			ComponentEntity entity = new ComponentEntity();
			entity.getComponents().add( component1 );
			entity.getComponents().add( component2 );
			entityManager.persist( entity );
			entityManager.getTransaction().commit();
			id = entity.getId();

			// Revision 2 - Change component name inline.
			// This effectively changes equality and hash of elment.
			entityManager.getTransaction().begin();
			component1.setName( "User1-Inline" );
			entityManager.merge( entity );
			entityManager.getTransaction().commit();

			// Revision 3 - Remove and add entity with same name.
			entityManager.getTransaction().begin();
			entity.getComponents().remove( component2 );
			Component component3 = new Component();
			component3.setName( "User2" );
			component3.setData( "Test3" );
			entity.getComponents().add( component3 );
			entityManager.merge( entity );
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( ComponentEntity.class, id ) );
	}

	@Test
	public void testCollectionHistory() {
		final ComponentEntity rev1 = getAuditReader().find( ComponentEntity.class, id, 1 );
		assertEquals( 2, rev1.getComponents().size() );
		assertEquals(
				TestTools.makeSet(
						new Component( "User1", "Test1" ),
						new Component( "User2", "Test2" )
				),
				rev1.getComponents()
		);

		final ComponentEntity rev2 = getAuditReader().find( ComponentEntity.class, id, 2 );
		assertEquals( 2, rev2.getComponents().size() );
		assertEquals(
				TestTools.makeSet(
						new Component( "User1-Inline", "Test1" ),
						new Component( "User2", "Test2" )
				),
				rev2.getComponents()
		);

		final ComponentEntity rev3 = getAuditReader().find( ComponentEntity.class, id, 3 );
		assertEquals( 2, rev3.getComponents().size() );
		assertEquals(
				TestTools.makeSet(
						new Component( "User1-Inline", "Test1" ),
						new Component( "User2", "Test3" )
				),
				rev3.getComponents()
		);
	}

	@Entity(name = "ComponentEntity")
	@Audited
	public static class ComponentEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@ElementCollection
		private Set<Component> components = new HashSet<Component>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Set<Component> getComponents() {
			return components;
		}

		public void setComponents(Set<Component> components) {
			this.components = components;
		}
	}

	@Audited
	@Embeddable
	public static class Component {
		private String name;
		private String data;

		Component() {

		}

		Component(String name, String data) {
			this.name = name;
			this.data = data;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		@Override
		public int hashCode() {
			return ( name != null ? name.hashCode() : 0 );
		}

		@Override
		public boolean equals(Object object) {
			if ( object == this ) {
				return true;
			}
			if ( object == null || !( object instanceof Component ) ) {
				return false;
			}
			Component that = (Component) object;
			return !( name != null ? !name.equals( that.name ) : that.name != null );
		}
	}
}
