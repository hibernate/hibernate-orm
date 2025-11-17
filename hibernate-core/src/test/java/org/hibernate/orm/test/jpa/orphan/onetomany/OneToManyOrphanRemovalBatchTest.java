/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		OneToManyOrphanRemovalBatchTest.EntityA.class,
		OneToManyOrphanRemovalBatchTest.EntityB.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5"))
@JiraKey("HHH-16112")
public class OneToManyOrphanRemovalBatchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityB entityB1 = new EntityB( "entity_b_1" );
			EntityB entityB2 = new EntityB( "entity_b_2" );
			session.persist( entityB1 );
			session.persist( entityB2 );
			EntityA entityA = new EntityA( "entity_a", List.of( entityB1, entityB2 ) );
			session.persist( entityA );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	public void testClearChildren(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA entityA = session.find( EntityA.class, 1L );
			entityA.getChildren().clear();
		} );
		scope.inTransaction( session -> {
			EntityA entityA = session.find( EntityA.class, 1L );
			assertEquals( 0, entityA.getChildren().size() );
			assertEquals( 0, session.createQuery( "from EntityB", EntityB.class ).getResultList().size() );
		} );
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(orphanRemoval = true)
		// JoinColumn is needed to trigger the update on the entity table
		// which by default has an update Expectation of 1 row
		@JoinColumn(name = "entitya_id")
		private List<EntityB> children;

		public EntityA() {
		}

		public EntityA(String name, List<EntityB> children) {
			this.name = name;
			this.children = children;
		}

		public List<EntityB> getChildren() {
			return children;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public EntityB() {
		}

		public EntityB(String name) {
			this.name = name;
		}
	}
}
