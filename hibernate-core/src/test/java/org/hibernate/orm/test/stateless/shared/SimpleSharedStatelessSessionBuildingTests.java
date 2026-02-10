/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.shared;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.orm.test.interceptor.StatefulInterceptor;
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
@DomainModel(annotatedClasses = SimpleSharedStatelessSessionBuildingTests.Something.class)
@SessionFactory(useCollectingStatementInspector = true, interceptorClass = StatefulInterceptor.class)
public class SimpleSharedStatelessSessionBuildingTests {

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
			try (var nested = (StatelessSessionImplementor) base.
					statelessWithOptions()
					.open()) {
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isSameAs( sqlCollector );
			}

			// 1. noStatementInspector
			try (var nested = (StatelessSessionImplementor) base
					.statelessWithOptions()
					.noStatementInspector()
					.open()) {
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isNotSameAs( sqlCollector );
				assertThat( nested.getJdbcSessionContext().getStatementInspector() ).isNotSameAs( appliedToBase );
			}

			// 2. statementInspector()
			try (var nested = (StatelessSessionImplementor) base
					.statelessWithOptions()
					.statementInspector()
					.open()) {
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
			try (var nested = (StatelessSessionImplementor) base.
					statelessWithOptions()
					.open()) {
				assertThat( nested.getInterceptor() ).isSameAs( sfInterceptor );
			}

			// 1. noInterceptor() - should use no (Empty)Interceptor
			try (var nested = (StatelessSessionImplementor) base
					.statelessWithOptions()
					.noInterceptor()
					.open()) {
				assertThat( nested.getInterceptor() ).isNotSameAs( sfInterceptor );
				assertThat( nested.getInterceptor() ).isNotSameAs( appliedToBase );
			}

			// 2. interceptor() - should share the interceptor from the base session
			try (var nested = (StatelessSessionImplementor) base
					.statelessWithOptions()
					.interceptor()
					.open()) {
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
			try (var statelessSession = statefulSession
					.statelessWithOptions()
					.connection()
					.open()) {
				statelessSession.insert( new Something( 1, "first" ) );
				assertSame( statefulSession.getTransaction(), statelessSession.getTransaction() );
				assertSame( statefulSession.getJdbcCoordinator(),
						((StatelessSessionImplementor) statelessSession).getJdbcCoordinator() );
				assertSame( statefulSession.getTransactionCompletionCallbacksImplementor(),
						((StatelessSessionImplementor) statelessSession).getTransactionCompletionCallbacksImplementor() );
			}
		} );
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

		// try from StatelessSession
		sqlCollector.clear();
		factoryScope.inStatelessTransaction( (statelessSession) -> {
			try (var session = statelessSession
					.statelessWithOptions()
					.connection()
					.open()) {
				session.insert( new Something( 2, "first" ) );
				assertSame( session.getTransaction(), statelessSession.getTransaction() );
				assertSame( ((StatelessSessionImplementor) session).getJdbcCoordinator(),
						statelessSession.getJdbcCoordinator() );
				assertSame( ((StatelessSessionImplementor) session).getTransactionCompletionCallbacksImplementor(),
						statelessSession.getTransactionCompletionCallbacksImplementor() );
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
