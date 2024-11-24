/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests that an empty embeddable that is nested inside an embeddable is initialized.
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-11926")
public class EmptyInitializedNestedCompositesTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentEmptyNestedEmbeddedOwner.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, Boolean.valueOf( true ) );
	}

	/**
	 * Test empty nested composite initialization.
	 */
	@Test
	@FailureExpected( jiraKey = "HHH-11926" )
	public void testCompositesEmpty() {
		Session s = openSession();
		try {
			s.getTransaction().begin();

			ComponentEmptyNestedEmbeddedOwner owner = new ComponentEmptyNestedEmbeddedOwner();
			s.persist( owner );

			s.flush();
			s.getTransaction().commit();

			s.clear();
			s.getTransaction().begin();
			owner = s.get( ComponentEmptyNestedEmbeddedOwner.class, owner.getId() );
			assertNotNull( owner.getEmbedded() );
			assertNotNull( owner.getEmbedded().getNestedEmbedded() );

			s.getTransaction().rollback();
		}
		finally {
			s.close();
		}
	}

	@Entity(name = "EmptyNestedOwner")
	public static class ComponentEmptyNestedEmbeddedOwner {

		@Id
		@GeneratedValue
		private Integer id;

		private EmptyNestedEmbeddedContainer embedded;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EmptyNestedEmbeddedContainer getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmptyNestedEmbeddedContainer embedded) {
			this.embedded = embedded;
		}

	}

	@Embeddable
	public static class EmptyNestedEmbeddedContainer {
		public ComponentEmptyEmbedded getNestedEmbedded() {
			return nestedEmbedded;
		}

		public void setNestedEmbedded(ComponentEmptyEmbedded nestedEmbedded) {
			this.nestedEmbedded = nestedEmbedded;
		}

		private ComponentEmptyEmbedded nestedEmbedded;
	}
}
