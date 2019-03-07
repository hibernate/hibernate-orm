/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.hashcode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11063")
public class ComponentCollectionHashcodeChangeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ComponentEntity.class, Component.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
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
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( ComponentEntity.class, id ) );
	}

	@DynamicTest
	public void testCollectionHistory() {
		final Component componentUser1Test1 = new Component( "User1", "Test1" );
		final Component componentUser2Test2 = new Component( "User2", "Test2" );
		final Component componentUser2Test3 = new Component( "User2", "Test3" );
		final Component componentUser1InlineTest1 = new Component( "User1-Inline", "Test1" );

		final ComponentEntity rev1 = getAuditReader().find( ComponentEntity.class, id, 1 );
		assertThat( rev1.getComponents(), containsInAnyOrder( componentUser1Test1, componentUser2Test2 ) );

		final ComponentEntity rev2 = getAuditReader().find( ComponentEntity.class, id, 2 );
		assertThat( rev2.getComponents(), containsInAnyOrder( componentUser1InlineTest1, componentUser2Test2 ) );

		final ComponentEntity rev3 = getAuditReader().find( ComponentEntity.class, id, 3 );
		assertThat( rev3.getComponents(), containsInAnyOrder( componentUser1InlineTest1, componentUser2Test3 ) );
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
