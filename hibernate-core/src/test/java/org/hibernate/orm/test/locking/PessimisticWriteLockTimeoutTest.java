/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.LockModeType;
import org.hibernate.Timeouts;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.Query;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = A.class)
@SessionFactory(useCollectingStatementInspector = true)
public class PessimisticWriteLockTimeoutTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new A() );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(SQLServerDialect.class)
	public void testNoWait(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			//noinspection removal
			session.createQuery( "select a from A a", A.class )
					.unwrap( Query.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setTimeout( Timeouts.NO_WAIT )
					.list();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "nowait" );
		} );
	}

	@Test
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void testSkipLocked(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			//noinspection removal
			session.createQuery("select a from A a", A.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setTimeout( Timeouts.SKIP_LOCKED )
					.list();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "skip locked" );
		} );
	}
}
