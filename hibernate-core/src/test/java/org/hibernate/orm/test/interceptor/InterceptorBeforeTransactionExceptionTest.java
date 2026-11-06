/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that an exception thrown in {@link Interceptor#beforeTransactionCompletion}
 * aborts the commit rather than being silently swallowed.
 */
@DomainModel(annotatedClasses = InterceptorBeforeTransactionExceptionTest.TestEntity.class)
@SessionFactory
public class InterceptorBeforeTransactionExceptionTest {

	@Test
	public void exceptionInBeforeCompletionShouldPreventCommit(SessionFactoryScope factoryScope) {
		final long entityId;

		// persist an entity so we have something to verify against
		try (var session = factoryScope.getSessionFactory().openSession()) {
			session.getTransaction().begin();
			var entity = new TestEntity( "initial" );
			session.persist( entity );
			session.getTransaction().commit();
			entityId = entity.getId();
		}

		// now attempt a modification with an interceptor that throws before commit
		assertThrows( RuntimeException.class, () -> {
			try (var session = factoryScope.getSessionFactory()
					.withOptions()
					.interceptor( new ExceptionThrowingInterceptor() )
					.openSession()) {
				session.getTransaction().begin();
				var entity = session.find( TestEntity.class, entityId );
				entity.setName( "modified" );
				session.getTransaction().commit();
			}
		} );

		// verify the modification was NOT committed
		try (var session = factoryScope.getSessionFactory().openSession()) {
			var entity = session.find( TestEntity.class, entityId );
			assertNotNull( entity );
			assertNull( entity.getName().equals( "modified" ) ? "modified" : null,
					"Transaction should have been rolled back, but the entity was modified" );
		}
	}

	private static class ExceptionThrowingInterceptor implements Interceptor {
		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			throw new RuntimeException( "Interceptor vetoes the commit" );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;
		private String name;

		TestEntity() {
		}

		TestEntity(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
