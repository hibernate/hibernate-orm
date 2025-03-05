/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@DomainModel(
		annotatedClasses = {
				EmbeddableWithIdenticallyNamedAssociationTest.EntityA.class,
				EmbeddableWithIdenticallyNamedAssociationTest.EntityB.class,
				EmbeddableWithIdenticallyNamedAssociationTest.EmbeddableB.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16209")
public class EmbeddableWithIdenticallyNamedAssociationTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		EntityA a1 = new EntityA();
		a1.setId( 1 );

		EntityB b1 = new EntityB();
		b1.setId( 1 );
		b1.setEntityA( a1 );
		a1.setEntityB( b1 );

		EntityA a2 = new EntityA();
		a2.setId( 2 );
		EntityB b2 = new EntityB();
		b2.setId( 2 );
		b2.setEntityA( a2 );
		a2.setEntityB( b2 );

		EmbeddableB embeddableB = new EmbeddableB();
		embeddableB.setEntityA( a2 );
		b1.setEmbeddableB( embeddableB );

		EmbeddableA embeddableA = new EmbeddableA();
		embeddableA.setEntityB( b1 );
		a2.setEmbeddableA( embeddableA );

		scope.inTransaction( session -> {
			session.persist( a1 );
			session.persist( a2 );
			session.persist( b1 );
			session.persist( b2 );

			assertEntityContent( a1, a2, b1, b2 );
		} );
	}

	private void assertEntityContent(EntityA a1, EntityA a2, EntityB b1, EntityB b2) {
		assertThat( a1 ).isNotNull();
		assertThat( a2 ).isNotNull();
		assertThat( b1 ).isNotNull();
		assertThat( b2 ).isNotNull();

		assertThat( b1.getEntityA() ).isEqualTo( a1 );
		assertThat( a1.getEntityB() ).isEqualTo( b1 );

		assertThat( b2.getEntityA() ).isEqualTo( a2 );
		assertThat( a2.getEntityB() ).isEqualTo( b2 );

		assertThat( b1.getEmbeddableB() ).isNotNull();
		assertThat( b1.getEmbeddableB().getEntityA() ).isEqualTo( a2 );
		assertThat( a2.getEmbeddableA() ).isNotNull();
		assertThat( a2.getEmbeddableA().getEntityB() ).isEqualTo( b1 );
	}

	@Test
	public void testGetEntities(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a1 = session.get( EntityA.class, 1 );
			EntityA a2 = session.get( EntityA.class, 2 );
			EntityB b1 = session.get( EntityB.class, 1 );
			EntityB b2 = session.get( EntityB.class, 2 );

			// Run the *exact* same assertions we ran just after persisting.
			// Entity content should be identical, but the bug is: it's not.
			assertEntityContent(a1, a2, b1, b2);
		} );
	}

	@Entity(name = "entityA")
	public static class EntityA {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "entityA")
		private EntityB entityB;

		@Embedded
		private EmbeddableA embeddableA;

		@Override
		public String toString() {
			return "EntityB{" +
					"id=" + id +
					", entityB =" + entityB.getId() +
					", embeddableA=" + embeddableA +
					'}';
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB entityB) {
			this.entityB = entityB;
		}

		public EmbeddableA getEmbeddableA() {
			return embeddableA;
		}

		public void setEmbeddableA(EmbeddableA embeddableA) {
			this.embeddableA = embeddableA;
		}
	}

	@Embeddable
	public static class EmbeddableA {
		@OneToOne(mappedBy = "embeddableB.entityA")
		private EntityB entityB;

		@Override
		public String toString() {
			return "EmbeddableA{" +
					", entityB=" + entityB.getId() +
					'}';
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB a) {
			this.entityB = a;
		}
	}

	@Entity(name = "entityB")
	public static class EntityB {
		@Id
		private Integer id;

		@OneToOne
		@JoinColumn(name = "entityA_id")
		private EntityA entityA;

		@Embedded
		private EmbeddableB embeddableB;

		@Override
		public String toString() {
			return "EntityB{" +
					"id=" + id +
					", entityA=" + entityA.getId() +
					", embeddableB=" + embeddableB +
					'}';
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA a) {
			this.entityA = a;
		}

		public EmbeddableB getEmbeddableB() {
			return embeddableB;
		}

		public void setEmbeddableB(EmbeddableB embeddableB) {
			this.embeddableB = embeddableB;
		}
	}

	@Embeddable
	public static class EmbeddableB {
		@OneToOne
		@JoinColumn(name = "emb_entityA_id")
		private EntityA entityA;

		@Override
		public String toString() {
			return "EmbeddableB{" +
					", entityA=" + entityA.getId() +
					'}';
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA a) {
			this.entityA = a;
		}
	}

}
