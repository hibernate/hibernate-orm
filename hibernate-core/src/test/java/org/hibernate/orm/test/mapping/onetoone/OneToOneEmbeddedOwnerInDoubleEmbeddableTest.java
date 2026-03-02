/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-20010")
@DomainModel(
		annotatedClasses = {
				OneToOneEmbeddedOwnerInNestedEmbeddableTest.EntityA.class,
				OneToOneEmbeddedOwnerInNestedEmbeddableTest.EntityC.class,
		}
)
@SessionFactory
class OneToOneEmbeddedOwnerInNestedEmbeddableTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityC entityC = new EntityC( 1 );
			final EntityA entityA = new EntityA( 1, entityC );
			entityC.setEntityA( entityA );
			session.persist( entityC );
			session.persist( entityA );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testLoadByPrimaryKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = session.find( EntityA.class, 1 );
			assertNotNull( entityA );
			assertNotNull( entityA.getEmbedded1() );
			assertNotNull( entityA.getEmbedded1().getEmbedded2() );
			assertNotNull( entityA.getEmbedded1().getEmbedded2().getEntityC() );
			assertEquals( 1, entityA.getEmbedded1().getEmbedded2().getEntityC().getId() );
		} );
	}

	@Test
	void testLoadEntityCByPrimaryKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityC entityC = session.find( EntityC.class, 1 );
			assertNotNull( entityC );
			assertEquals( 1, entityC.getId() );
		} );
	}

	@Test
	void testLoadViaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = session.createQuery(
							"from EntityA a where a.id = :id", EntityA.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull();
			assertNotNull( entityA );
			assertNotNull( entityA.getEmbedded1().getEmbedded2().getEntityC() );
		} );
	}

	@Entity(name = "EntityA")
	@Table(name = "entity_a_hhh20010")
	public static class EntityA {

		@Id
		private Integer id;

		@Embedded
		private Embedded1 embedded1;

		public EntityA() {
		}

		public EntityA(Integer id, EntityC entityC) {
			this.id = id;
			final Embedded2 emb2 = new Embedded2();
			emb2.setEntityC( entityC );
			final Embedded1 emb1 = new Embedded1();
			emb1.setEmbedded2( emb2 );
			this.embedded1 = emb1;
		}

		public Integer getId() {
			return id;
		}

		public Embedded1 getEmbedded1() {
			return embedded1;
		}

		public void setEmbedded1(Embedded1 embedded1) {
			this.embedded1 = embedded1;
		}
	}

	@Embeddable
	public static class Embedded1 {

		@Embedded
		private Embedded2 embedded2;

		public Embedded2 getEmbedded2() {
			return embedded2;
		}

		public void setEmbedded2(Embedded2 embedded2) {
			this.embedded2 = embedded2;
		}
	}

	@Embeddable
	public static class Embedded2 {

		@OneToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "entity_c_id")
		private EntityC entityC;

		public EntityC getEntityC() {
			return entityC;
		}

		public void setEntityC(EntityC entityC) {
			this.entityC = entityC;
		}
	}

	@Entity(name = "EntityC")
	@Table(name = "entity_c_hhh20010")
	public static class EntityC {

		@Id
		private Integer id;

		@OneToOne(mappedBy = "embedded1.embedded2.entityC", fetch = FetchType.LAZY)
		private EntityA entityA;

		public EntityC() {
		}

		public EntityC(Integer id) {
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
