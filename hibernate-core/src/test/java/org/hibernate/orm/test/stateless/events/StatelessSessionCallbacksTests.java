/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.events;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = StatelessSessionCallbacksTests.Person.class)
@SessionFactory
public class StatelessSessionCallbacksTests {

	@Test
	void simpleSession(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Person john = new Person( 1, "John" );
			session.persist( john );
		} );

		checkCallbackCounts( 1, 0, 0 );

		factoryScope.inTransaction( (session) -> {
			final Person john = session.find( Person.class, 1 );
			john.name = "Jonathan";
		} );

		checkCallbackCounts( 1, 1, 0 );

		factoryScope.inTransaction( (session) -> {
			final Person john = session.find( Person.class, 1 );
			session.remove( john );
		} );

		checkCallbackCounts( 1, 1, 1 );
	}

	@Test
	void simpleStatelessSession(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			final Person john = new Person( 1, "John" );
			session.insert( john );
		} );

		checkCallbackCounts( 1, 0, 0 );

		factoryScope.inStatelessTransaction( (session) -> {
			final Person john = session.get( Person.class, 1 );
			john.name = "Jonathan";
			session.update( john );
		} );

		checkCallbackCounts( 1, 1, 0 );

		factoryScope.inStatelessTransaction( (session) -> {
			final Person john = session.get( Person.class, 1 );
			session.delete( john );
		} );

		checkCallbackCounts( 1, 1, 1 );
	}

	private void checkCallbackCounts(int insert, int update, int delete) {
		assertThat( Person.beforeInsertCalls ).isEqualTo( insert );
		assertThat( Person.afterInsertCalls ).isEqualTo( insert );

		assertThat( Person.beforeUpdateCalls ).isEqualTo( update );
		assertThat( Person.afterUpdateCalls ).isEqualTo( update );

		assertThat( Person.beforeDeleteCalls ).isEqualTo( delete );
		assertThat( Person.afterDeleteCalls ).isEqualTo( delete );

	}

	@BeforeEach
	void setUp() {
		Person.resetCallbackState();
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		public static int beforeInsertCalls;
		public static int afterInsertCalls;

		public static int beforeUpdateCalls;
		public static int afterUpdateCalls;

		public static int beforeDeleteCalls;
		public static int afterDeleteCalls;

		@Id
		private Integer id;
		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@PrePersist
		public void beforeInsert() {
			beforeInsertCalls++;
		}

		@PostPersist
		public void afterInsert() {
			afterInsertCalls++;
		}

		@PreUpdate
		public void beforeUpdate() {
			beforeUpdateCalls++;
		}

		@PostUpdate
		public void afterUpdate() {
			afterUpdateCalls++;
		}

		@PreRemove
		public void beforeDelete() {
			beforeDeleteCalls++;
		}

		@PostRemove
		public void afterDelete() {
			afterDeleteCalls++;
		}

		public static void resetCallbackState() {
			beforeInsertCalls = 0;
			afterInsertCalls = 0;

			beforeUpdateCalls = 0;
			afterUpdateCalls = 0;

			beforeDeleteCalls = 0;
			afterDeleteCalls = 0;
		}
	}
}
