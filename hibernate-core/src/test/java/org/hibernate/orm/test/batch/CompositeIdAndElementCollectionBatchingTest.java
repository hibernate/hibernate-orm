/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@DomainModel(
		annotatedClasses = {
				CompositeIdAndElementCollectionBatchingTest.EntityA.class,
				CompositeIdAndElementCollectionBatchingTest.EntityB.class,
				CompositeIdAndElementCollectionBatchingTest.EmbeddableA.class,
				CompositeIdAndElementCollectionBatchingTest.EntityId.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "2")
)
@JiraKey("HHH-16740")
class CompositeIdAndElementCollectionBatchingTest {

	private static final EntityB ENTITY_B = new EntityB( 1L );
	private static final EntityA ENTITY_A = new EntityA(
			new EntityId( "EntityA", ENTITY_B ),
			Collections.singleton( new EmbeddableA( "EmbeddableA" ) )
	);
	private static final EntityA ENTITY_A2 = new EntityA(
			new EntityId( "EntityA2", ENTITY_B ),
			Collections.singleton( new EmbeddableA( "EmbeddableB" ) )
	);

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( ENTITY_B );
					session.persist( ENTITY_A );
					session.persist( ENTITY_A2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA a = session.find( EntityA.class, ENTITY_A.id );
					assertThat( a.elementCollection ).hasSize( 1 );
				}
		);
	}

	@Test
	void testSelect2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA a = session.find( EntityA.class, ENTITY_A.id );
					EntityA a2 = session.find( EntityA.class, ENTITY_A2.id );
					assertThat( a.elementCollection ).hasSize( 1 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@EmbeddedId
		private EntityId id;

		@ElementCollection
		public Set<EmbeddableA> elementCollection = new HashSet<>();

		public EntityA() {
		}

		public EntityA(EntityId id, Set<EmbeddableA> elementCollection) {
			this.id = id;
			this.elementCollection = elementCollection;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;

		public EntityB() {
		}

		public EntityB(Long id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class EntityId implements Serializable {
		private String id1;
		@ManyToOne
		@MapsId
		private EntityB id2;

		public EntityId() {
		}

		public EntityId(String id1, EntityB id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}

	@Embeddable
	public static class EmbeddableA {
		private String name;

		public EmbeddableA() {
		}

		public EmbeddableA(String name) {
			this.name = name;
		}
	}

}
