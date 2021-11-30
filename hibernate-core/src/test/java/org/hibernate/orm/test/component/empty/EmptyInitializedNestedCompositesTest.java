/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.component.empty;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Tests that an empty embeddable that is nested inside an embeddable is initialized.
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-11926")
@DomainModel(
		annotatedClasses = EmptyInitializedNestedCompositesTest.ComponentEmptyNestedEmbeddedOwner.class
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.CREATE_EMPTY_COMPOSITES_ENABLED, value = "true")
)
public class EmptyInitializedNestedCompositesTest {

	/**
	 * Test empty nested composite initialization.
	 */
	@Test
	public void testCompositesEmpty(SessionFactoryScope scope) {

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						ComponentEmptyNestedEmbeddedOwner owner = new ComponentEmptyNestedEmbeddedOwner();
						session.persist( owner );

						session.flush();
						session.getTransaction().commit();

						session.clear();
						session.getTransaction().begin();
						owner = session.get( ComponentEmptyNestedEmbeddedOwner.class, owner.getId() );
						assertNotNull( owner.getEmbedded() );
						assertNotNull( owner.getEmbedded().getNestedEmbedded() );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
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
