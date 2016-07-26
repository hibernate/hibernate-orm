/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclass;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.Session;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class JoinedSubclassWithEmbeddableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BaseEntity.class, ConcreteEntity.class};
	}

	@Before
	public void setUp() {
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				ConcreteEntity entity = new ConcreteEntity();
				entity.setId( 1L );
				entity.setField( "field_base" );
				EmbeddedValue embeddedValue = new EmbeddedValue();
				embeddedValue.setField( "field_embedded" );
				entity.setEmbedded( embeddedValue );

				session.save( entity );
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10920")
	public void testEmbeddedFieldIsNotNull() {
		try (Session session = openSession()) {
			session.beginTransaction();
			try {
				final ConcreteEntity entity = session.get( ConcreteEntity.class, 1L );
				assertThat( entity.getEmbedded().getField(), is( "field_embedded" ) );
				assertThat( entity.getField(), is( "field_base" ) );
			}
			finally {
				session.getTransaction().rollback();
			}
		}
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class BaseEntity {
		@Id
		private Long id;

		private String field;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	@Entity(name = "ConcreteEntity")
	public static class ConcreteEntity extends BaseEntity {
		private EmbeddedValue embeddedValue;

		public EmbeddedValue getEmbedded() {
			return embeddedValue;
		}

		public void setEmbedded(EmbeddedValue embeddedValue) {
			this.embeddedValue = embeddedValue;
		}
	}

	@Embeddable
	public static class EmbeddedValue {
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
