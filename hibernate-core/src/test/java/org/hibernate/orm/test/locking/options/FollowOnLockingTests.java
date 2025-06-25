/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.LockModeType;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class, Report.class})
@SessionFactory(useCollectingStatementInspector = true)
@Tag("db-locking")
public class FollowOnLockingTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		Helper.createTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testFindBaseline(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );
		} );
	}

	@Test
	void testFindWithForced(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE, Locking.FollowOn.FORCE );

			if ( usesTableHints( session.getDialect() ) ) {
				// t-sql
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );
			}
			else {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), Helper.Table.BOOKS );
			}
		} );
	}

	private boolean usesTableHints(Dialect dialect) {
		return dialect.getLockingSupport().getMetadata().getPessimisticLockStyle() == PessimisticLockStyle.TABLE_HINT;
	}

	@Test
	void testFindWithForcedAsHint(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Map<String, Object> hints = Map.of( HibernateHints.HINT_FOLLOW_ON_STRATEGY, Locking.FollowOn.FORCE );
			session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE, hints );

			if ( usesTableHints( session.getDialect() ) ) {
				// t-sql
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );
			}
			else {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), Helper.Table.BOOKS );
			}
		} );
	}

	@Test
	void testFindWithForcedAsHintName(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Map<String, Object> hints = Map.of( HibernateHints.HINT_FOLLOW_ON_STRATEGY, Locking.FollowOn.FORCE.name() );
			session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE, hints );

			if ( usesTableHints( session.getDialect() ) ) {
				// t-sql
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );
			}
			else {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), Helper.Table.BOOKS );
			}
		} );
	}

	@Test
	void testFindWithForcedAsLegacyHint(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Map<String, Object> hints = Map.of( HibernateHints.HINT_FOLLOW_ON_LOCKING, true );
			session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE, hints );

			if ( usesTableHints( session.getDialect() ) ) {
				// t-sql
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );
			}
			else {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), Helper.Table.BOOKS );
			}
		} );
	}
}
