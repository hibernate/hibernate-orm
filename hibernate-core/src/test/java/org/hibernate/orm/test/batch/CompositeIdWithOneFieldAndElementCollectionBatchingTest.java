/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

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

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				CompositeIdWithOneFieldAndElementCollectionBatchingTest.EntityA.class,
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "2")
)
@JiraKey("HHH-16750")
class CompositeIdWithOneFieldAndElementCollectionBatchingTest {

	private static final EntityA ENTITY_A = new EntityA(
			new EntityId( "EntityA" ),
			Collections.singleton( new EmbeddableA( "EmbeddableA" ) )
	);

	private static final EntityA ENTITY_A2 = new EntityA(
			new EntityId( "EntityB" ),
			Collections.singleton( new EmbeddableA( "EmbeddableB" ) )
	);

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( ENTITY_A );
					session.persist( ENTITY_A2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
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

	@Embeddable
	public static class EntityId implements Serializable {
		private String id1;

		public EntityId() {
		}

		public EntityId(String id1) {
			this.id1 = id1;
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
