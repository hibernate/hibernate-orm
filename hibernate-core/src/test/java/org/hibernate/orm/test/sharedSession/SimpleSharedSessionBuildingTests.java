/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sharedSession;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = SimpleSharedSessionBuildingTests.Something.class)
@SessionFactory(useCollectingStatementInspector = true, interceptorClass = DummyInterceptor.class)
public class SimpleSharedSessionBuildingTests {

	@Test
	void testStatementInspector(SessionFactoryScope factoryScope) {
		final var sqlCollector = factoryScope.getCollectingStatementInspector();

		// apply a special StatementInspector to the base and check the behavior of the various options
		final var appliedToBase = new StatementInspectorImpl( "Applied to base" );

		try (var base = factoryScope.getSessionFactory()
				.withOptions()
				.statementInspector( appliedToBase )
				.openSession()) {

			// baseline:
			try (var nested = (SessionImplementor) base.
					sessionWithOptions()
					.openSession()) {
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isSameAs( sqlCollector );
			}

			// 1. noStatementInspector
			try (var nested = (SessionImplementor) base
					.sessionWithOptions()
					.noStatementInspector()
					.openSession()) {
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isNotSameAs( sqlCollector );
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isNotSameAs( appliedToBase );
			}

			// 2. statementInspector()
			try (var nested = (SessionImplementor) base
					.sessionWithOptions()
					.statementInspector()
					.openSession()) {
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isSameAs( appliedToBase );
			}
		}
	}

	@Test
	void testInterceptor(SessionFactoryScope factoryScope) {
		final var sfInterceptor = factoryScope.getSessionFactory().getSessionFactoryOptions().getInterceptor();

		// apply a special StatementInspector to the base and check the behavior of the various options
		final var appliedToBase = new InterceptorImpl( "Applied to base" );

		try (var base = factoryScope.getSessionFactory()
				.withOptions()
				.interceptor( appliedToBase )
				.openSession()) {

			// baseline - should use the Interceptor from SF
			try (var nested = (SessionImplementor) base.
					sessionWithOptions()
					.openSession()) {
				assertThat( nested.getInterceptor() ).isSameAs( sfInterceptor );
			}

			// 1. noInterceptor() - should use no (Empty)Interceptor
			try (var nested = (SessionImplementor) base
					.sessionWithOptions()
					.noInterceptor()
					.openSession()) {
				assertThat( nested.getInterceptor() ).isNotSameAs( sfInterceptor );
				assertThat( nested.getInterceptor() ).isNotSameAs( appliedToBase );
			}

			// 2. interceptor() - should share the interceptor from the base session
			try (var nested = (SessionImplementor) base
					.sessionWithOptions()
					.interceptor()
					.openSession()) {
				assertThat( nested.getInterceptor() ).isSameAs( appliedToBase );
			}
		}
	}

	@Test
	void testConnectionAndTransactionSharing(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (parentSession) -> {
			assertThat( parentSession.getHibernateFlushMode() ).isEqualTo( FlushMode.AUTO );

			// child stateful session
			try (SessionImplementor childSession = (SessionImplementor) parentSession
					.sessionWithOptions()
					.connection()
					.openSession()) {
				assertThat( childSession.getHibernateFlushMode() )
						.isEqualTo( FlushMode.AUTO );
				assertThat( childSession.getTransaction() )
						.isSameAs( parentSession.getTransaction() );
				assertThat( childSession.getJdbcCoordinator() )
						.isSameAs( parentSession.getJdbcCoordinator() );
				assertThat( childSession.getTransactionCompletionCallbacksImplementor() )
						.isSameAs( parentSession.getTransactionCompletionCallbacksImplementor() );
			}

			// child stateless session
			try (StatelessSessionImplementor childSession = (StatelessSessionImplementor) parentSession
					.statelessWithOptions()
					.connection()
					.open()) {
				assertThat( childSession.getHibernateFlushMode() )
						.isEqualTo( FlushMode.AUTO );
				assertThat( childSession.getTransaction() )
						.isSameAs( parentSession.getTransaction() );
				assertThat( childSession.getJdbcCoordinator() )
						.isSameAs( parentSession.getJdbcCoordinator() );
				assertThat( childSession.getTransactionCompletionCallbacksImplementor() )
						.isSameAs( parentSession.getTransactionCompletionCallbacksImplementor() );
			}
		} );
	}

	@Test
	void testClosePropagation(SessionFactoryScope factoryScope) {
		final MutableObject<SharedSessionContractImplementor> parentSessionRef = new MutableObject<>();
		final MutableObject<SharedSessionContractImplementor> childSessionRef = new MutableObject<>();

		factoryScope.inTransaction( (parentSession) -> {
			parentSessionRef.set( parentSession );

			var childSession = (SessionImplementor) parentSession
					.sessionWithOptions()
					.connection()
					.openSession();
			childSessionRef.set( childSession );
		} );

		assertThat( parentSessionRef.get().isClosed() ).isTrue();
		assertThat( childSessionRef.get().isClosed() ).isTrue();

		parentSessionRef.set( null );
		childSessionRef.set( null );

		factoryScope.inTransaction( (parentSession) -> {
			parentSessionRef.set( parentSession );

			var childSession = (StatelessSessionImplementor) parentSession
					.statelessWithOptions()
					.connection()
					.open();
			childSessionRef.set( childSession );
		} );

		assertThat( parentSessionRef.get().isClosed() ).isTrue();
		assertThat( childSessionRef.get().isClosed() ).isTrue();
	}

	/**
	 * NOTE: builds on assertions from {@link #testConnectionAndTransactionSharing}
	 */
	@Test
	void testAutoFlushStatefulChild(SessionFactoryScope factoryScope) {
		final var sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (parentSession) -> {
			try (SessionImplementor childSession = (SessionImplementor) parentSession
					.sessionWithOptions()
					.connection()
					.openSession()) {
				// persist an entity through the child session -
				// should be auto flushed (technically as part of the try-with-resources close of the child session)
				childSession.persist( new Something( 1, "first" ) );
			}
		} );

		// make sure the flush and insert happened
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		factoryScope.inTransaction( (session) -> {
			final Something created = session.find( Something.class, 1 );
			assertThat( created ).isNotNull();
		} );

	}

	/**
	 * NOTE: builds on assertions from {@link #testConnectionAndTransactionSharing}
	 * and {@linkplain #testClosePropagation}
	 */
	@Test
	void testAutoFlushStatefulChildNoClose(SessionFactoryScope factoryScope) {
		final var sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (parentSession) -> {
			SessionImplementor childSession = (SessionImplementor) parentSession
					.sessionWithOptions()
					.connection()
					.openSession();

			// persist an entity through the shared/child session.
			// then make sure the auto-flush of the parent session
			// propagates to the shared/child
			childSession.persist( new Something( 1, "first" ) );
		} );

		// make sure the flush and insert happened
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		factoryScope.inTransaction( (session) -> {
			final Something created = session.find( Something.class, 1 );
			assertThat( created ).isNotNull();
		} );
	}



	/**
	 * NOTE: builds on assertions from {@link #testConnectionAndTransactionSharing}
	 */
	@Test
	void testAutoFlushStatelessChild(SessionFactoryScope factoryScope) {
		final var sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inStatelessTransaction( (parentSession) -> {
			try (SessionImplementor childSession = (SessionImplementor) parentSession
					.sessionWithOptions()
					.connection()
					.openSession()) {
				// persist an entity through the child session -
				// should be auto flushed (technically as part of the try-with-resources close of the child session)
				childSession.persist( new Something( 1, "first" ) );
			}
		} );

		// make sure the flush and insert happened
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		factoryScope.inTransaction( (session) -> {
			final Something created = session.find( Something.class, 1 );
			assertThat( created ).isNotNull();
		} );
	}

	/**
	 * NOTE: builds on assertions from {@link #testConnectionAndTransactionSharing}
	 * and {@linkplain #testClosePropagation}
	 */
	@Test
	void testAutoFlushStatelessChildNoClose(SessionFactoryScope factoryScope) {
		final var sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inStatelessTransaction( (parentSession) -> {
			SessionImplementor childSession = (SessionImplementor) parentSession
					.sessionWithOptions()
					.connection()
					.openSession();

			// persist an entity through the shared/child session.
			// then make sure the auto-flush of the parent session
			// propagates to the shared/child
			childSession.persist( new Something( 1, "first" ) );
		} );

		// make sure the flush and insert happened
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		factoryScope.inTransaction( (session) -> {
			final Something created = session.find( Something.class, 1 );
			assertThat( created ).isNotNull();
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity(name="Something")
	@Table(name="somethings")
	public static class Something {
		@Id
		private Integer id;
		private String name;

		public Something() {
		}

		public Something(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public record StatementInspectorImpl(String name) implements StatementInspector {
		@Override
		public String inspect(String sql) {
			return sql;
		}
	}

	public record InterceptorImpl(String name) implements Interceptor {
	}

	public static class SessionListener implements SessionEventListener {
		private boolean closed;

		public boolean wasClosed() {
			return closed;
		}

		@Override
		public void end() {
			closed = true;
		}
	}
}
