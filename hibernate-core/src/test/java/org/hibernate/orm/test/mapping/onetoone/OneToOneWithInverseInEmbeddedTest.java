/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.onetoone;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@TestForIssue(jiraKey = "HHH-15786")
public class OneToOneWithInverseInEmbeddedTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	@Before
	public void prepare() {
		inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			EntityB entityB = new EntityB( 2 );
			entityA.setEntityB( entityB );
			entityB.getEmbedded().setEntityA( entityA );
			s.persist( entityA );
			s.persist( entityB );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( s -> {
			s.createMutationQuery( "delete entityb" ).executeUpdate();
			s.createMutationQuery( "delete entitya" ).executeUpdate();
		} );
	}

	@Test
	public void testUpdate() {
		inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, "1" );

			assertThat( entityA ).isNotNull();

			EntityB anotherEntityB = new EntityB( 3 );
			entityA.getEntityB().getEmbedded().setEntityA( null );
			entityA.setEntityB( anotherEntityB );
			anotherEntityB.getEmbedded().setEntityA( entityA );
		} );

		inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, "1" );

			assertThat( entityA ).isNotNull();
			assertThat( entityA.getEntityB() ).isNotNull();
			assertThat( entityA.getEntityB().getId() ).isEqualTo( 3 );
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private EntityB entityB;

		public EntityA() {
		}

		private EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB entityB) {
			this.entityB = entityB;
		}
	}

	@Embeddable
	public static class EmbeddedValue implements Serializable {
		@OneToOne
		private EntityA entityA;

		public EmbeddedValue() {
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}
	}

	@Entity(name = "entityb")
	public static class EntityB {
		@Id
		private Integer id;

		private String name;

		@Embedded
		private EmbeddedValue embedded = new EmbeddedValue();

		public EntityB() {
		}

		private EntityB(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValue getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValue embedded) {
			this.embedded = embedded;
		}
	}
}
