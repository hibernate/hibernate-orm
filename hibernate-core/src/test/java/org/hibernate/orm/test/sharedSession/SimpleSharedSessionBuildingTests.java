/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sharedSession;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

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
	void testUsage(SessionFactoryScope factoryScope) {
		final var sqlCollector = factoryScope.getCollectingStatementInspector();

		// try from Session
		sqlCollector.clear();
		factoryScope.inTransaction( (statefulSession) -> {
			try (var session = statefulSession
					.sessionWithOptions()
					.connection()
					.openSession()) {
				session.persist( new Something( 1, "first" ) );
				assertSame( statefulSession.getTransaction(), session.getTransaction() );
				assertSame( statefulSession.getJdbcCoordinator(),
						((SessionImplementor) session).getJdbcCoordinator() );
				assertSame( statefulSession.getTransactionCompletionCallbacksImplementor(),
						((SessionImplementor) session).getTransactionCompletionCallbacksImplementor() );
				session.flush(); //TODO: should not be needed!
			}
		} );
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

		// try from StatelessSession
		sqlCollector.clear();
		factoryScope.inStatelessTransaction( (statelessSession) -> {
			try (var statefulSession = statelessSession
					.sessionWithOptions()
					.connection()
					.openSession()) {
				statefulSession.persist( new Something( 2, "first" ) );
				assertSame( statefulSession.getTransaction(), statelessSession.getTransaction() );
				assertSame( ((SessionImplementor) statefulSession).getJdbcCoordinator(),
						((StatelessSessionImplementor) statelessSession).getJdbcCoordinator() );
				assertSame( ((SessionImplementor) statefulSession).getTransactionCompletionCallbacksImplementor(),
						((StatelessSessionImplementor) statelessSession).getTransactionCompletionCallbacksImplementor() );
				statefulSession.flush(); //TODO: should not be needed!
			}
		} );
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
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
}
