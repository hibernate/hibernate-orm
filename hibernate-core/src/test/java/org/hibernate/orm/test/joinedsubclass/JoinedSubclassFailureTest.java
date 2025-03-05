/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.joinedsubclass;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.Serializable;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Chris Cranford
 */
@DomainModel(annotatedClasses = { JoinedSubclassFailureTest.EntityA.class, JoinedSubclassFailureTest.EntityB.class } )
@SessionFactory
public class JoinedSubclassFailureTest {

	@Test
	public void shouldNotCauseFailure(SessionFactoryScope scope) {
		// Store some data
		scope.inTransaction( session -> {
			EntityA a1 = new EntityA( new EntityId( 1L ), "a1" );
			session.persist( a1 );

			EntityA a2 = new EntityA( new EntityId( 2L ), "a2" );
			session.persist( a2 );

			EntityB b1 = new EntityB( new EntityId( 3L ), "b1" );
			session.persist( b1 );

			EntityB b2 = new EntityB( new EntityId( 4L ), "b2" );
			session.persist( b2 );
		} );

		scope.inSession( session -> {
			// Simulate building a slim version of the Envers HQL
			final StringBuilder query = new StringBuilder();
			query.append( "select e__.name " )
					.append( "from " ).append( EntityA.class.getName() ).append( " e__ " )
					.append( "where type( e__ ) = " ).append( EntityB.class.getName() ).append( " " )
					.append( "order by e__.name asc" );

			// Simulate the Envers test case assertion
			List<String> results = (List<String>) session.createQuery( query.toString() ).getResultList();
			assertThat( results, contains( "b1", "b2" ) );
		} );
	}

	@Embeddable
	public static class EntityId implements Serializable {
		private Long id;

		EntityId() {
		}

		EntityId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "EntityA")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class EntityA {
		@EmbeddedId
		private EntityId id;
		private String name;

		EntityA() {
		}

		EntityA(EntityId id, String name) {
			this.id = id;
			this.name = name;
		}

		public EntityId getId() {
			return id;
		}

		public void setId(EntityId id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB extends EntityA {

		EntityB() {
		}

		EntityB(EntityId id, String name) {
			super( id, name );
		}
	}
}
