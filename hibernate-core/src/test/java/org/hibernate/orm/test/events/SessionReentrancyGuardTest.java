/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = SessionReentrancyGuardTest.GuardedEntity.class)
@SessionFactory
public class SessionReentrancyGuardTest {
	private static final String ILLEGAL_REENTRANCY_MESSAGE =
			"Session method called from entity lifecycle callback or Interceptor method";

	private static Runnable lifecycleCallbackAction;

	@AfterEach
	void tearDown() {
		lifecycleCallbackAction = null;
	}

	@Test
	void sessionUseFromEntityLifecycleCallbackThrows(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().openSession()) {
			final var transaction = session.beginTransaction();
			lifecycleCallbackAction = () -> session.find( GuardedEntity.class, 1L );

			assertThatThrownBy( () -> session.persist( new GuardedEntity( 1L, "stateful" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );

			transaction.rollback();
		}
	}

	@Test
	void statelessSessionUseFromEntityLifecycleCallbackThrows(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().openStatelessSession()) {
			lifecycleCallbackAction = () -> session.find( GuardedEntity.class, 1L );

			assertThatThrownBy( () -> session.insert( new GuardedEntity( 1L, "stateless" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );
		}
	}

	@Test
	void sessionPersistUseFromEntityLifecycleCallbackThrows(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().openSession()) {
			final var transaction = session.beginTransaction();
			lifecycleCallbackAction = () -> session.persist( new GuardedEntity( 5L, "stateful-metadata" ) );

			assertThatThrownBy( () -> session.persist( new GuardedEntity( 3L, "stateful-metadata" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );

			transaction.rollback();
		}
	}

	@Test
	void sessionConfigurationUseFromEntityLifecycleCallbackThrows(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().openSession()) {
			lifecycleCallbackAction = () -> session.setCacheStoreMode(CacheStoreMode.REFRESH);

			assertThatThrownBy( () -> session.persist( new GuardedEntity( 3L, "stateless-configuration" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );
		}
	}

	@Test
	void statelessSessionConfigurationUseFromEntityLifecycleCallbackThrows(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().openStatelessSession()) {
			lifecycleCallbackAction = () -> session.setJdbcBatchSize(666);

			assertThatThrownBy( () -> session.insert( new GuardedEntity( 3L, "stateless-configuration" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );
		}
	}

	@Test
	void sessionUseFromInterceptorThrows(SessionFactoryScope scope) {
		final var interceptor = new ReentrantSessionInterceptor();
		try (var session = scope.getSessionFactory()
				.withOptions()
				.interceptor( interceptor )
				.openSession()) {
			interceptor.session = session;
			final var transaction = session.beginTransaction();

			assertThatThrownBy( () -> session.persist( new GuardedEntity( 2L, "interceptor" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );

			transaction.rollback();
		}
	}

	@Test
	void statelessSessionUseFromInterceptorThrows(SessionFactoryScope scope) {
		final var interceptor = new ReentrantStatelessInterceptor();
		final var builder = scope.getSessionFactory().withStatelessOptions();
		builder.interceptor( interceptor );
		try (var session = builder.openStatelessSession()) {
			interceptor.session = session;

			assertThatThrownBy( () -> session.insert( new GuardedEntity( 2L, "stateless-interceptor" ) ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( ILLEGAL_REENTRANCY_MESSAGE );
		}
	}

	@Entity(name = "GuardedEntity")
	public static class GuardedEntity {
		@Id
		private Long id;

		private String name;

		public GuardedEntity() {
		}

		public GuardedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@PrePersist
		public void prePersist() {
			if ( lifecycleCallbackAction != null ) {
				lifecycleCallbackAction.run();
			}
		}
	}

	private static class ReentrantSessionInterceptor implements Interceptor {
		private Session session;

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			session.find( GuardedEntity.class, id );
			return false;
		}
	}

	private static class ReentrantStatelessInterceptor implements Interceptor {
		private StatelessSession session;

		@Override
		public void onInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {
			session.find( GuardedEntity.class, id );
		}
	}
}
