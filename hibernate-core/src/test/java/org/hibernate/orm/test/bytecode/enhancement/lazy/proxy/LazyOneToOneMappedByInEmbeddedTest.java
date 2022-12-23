/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
@TestForIssue(jiraKey = "HHH-15606")
public class LazyOneToOneMappedByInEmbeddedTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	@Before
	public void prepare() {
		inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			EntityB entityB = new EntityB( 2 );
			entityA.getEmbedded().setEntityB( entityB );
			entityB.setEntityA( entityA );
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
	public void testGet() {
		inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, "1" );

			assertThat( entityA ).isNotNull();

			EmbeddedValue embedded = entityA.getEmbedded();

			assertThat( embedded ).isNotNull();
			assertThat( embedded.getEntityB() ).isNotNull();
			assertThat( embedded.getEntityB().getEntityA() ).isEqualTo( entityA );
		} );
	}

	@Test
	public void testGetReference() {
		inTransaction( s -> {
			EntityA entityA = s.getReference( EntityA.class, "1" );

			assertThat( entityA ).isNotNull();

			EmbeddedValue embedded = entityA.getEmbedded();

			assertThat( embedded ).isNotNull();
			assertThat( embedded.getEntityB() ).isNotNull();
			assertThat( embedded.getEntityB().getEntityA() ).isEqualTo( entityA );
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {
		@Id
		private Integer id;

		private String name;

		@Embedded
		private EmbeddedValue embedded = new EmbeddedValue();

		public EntityA() {
		}

		private EntityA(Integer id) {
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

	@Embeddable
	public static class EmbeddedValue implements Serializable {
		@OneToOne(mappedBy = "entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EmbeddedValue() {
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(
				EntityB entityB) {
			this.entityB = entityB;
		}
	}


	@Entity(name = "entityb")
	public static class EntityB {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private EntityA entityA;

		public EntityB() {
		}

		private EntityB(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}
	}
}
