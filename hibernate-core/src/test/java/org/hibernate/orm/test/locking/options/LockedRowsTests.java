/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Timeout;
import org.hibernate.PessimisticLockException;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.orm.AsyncExecutor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.Timeouts.NO_WAIT;
import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.hibernate.event.spi.LockEvent.ILLEGAL_SKIP_LOCKED;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@linkplain jakarta.persistence.Timeout}, including
 * "magic values".
 *
 * @implNote Tests with {@linkplain org.hibernate.Session#find} and
 * {@linkplain org.hibernate.Session#lock} as hopefully representative.
 *
 * @see org.hibernate.Timeouts#SKIP_LOCKED
 * @see org.hibernate.Timeouts#NO_WAIT
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class, Report.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19336" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19459" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
@Tag("db-locking")
public class LockedRowsTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
		Helper.createTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportNoWait.class)
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "no failure")
	void testFindNoWait(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			assert session.getDialect().supportsNoWait();
			session.find(Book.class,1, PESSIMISTIC_WRITE);

			factoryScope.inTransaction( (session2) -> {
				try {
					session2.find(Book.class,1, PESSIMISTIC_WRITE, NO_WAIT);
					fail("Expecting a failure due to locked rows and no-wait");
				}
				catch (PessimisticLockException expected) {
				}
			} );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportNoWait.class)
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "no failure")
	void testLockNoWait(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.find(Book.class,1, PESSIMISTIC_WRITE);

			factoryScope.inTransaction( (session2) -> {
				try {
					final Book book = session2.find( Book.class, 1 );
					session2.lock( book, PESSIMISTIC_WRITE, NO_WAIT );
					fail("Expecting a failure due to locked rows and no-wait");
				}
				catch (PessimisticLockException | PessimisticEntityLockException expected) {
				}
			} );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@SkipForDialect(
			dialectClass = MariaDBDialect.class,
			reason = "Cannot figure this out - it passes when run by itself, but fails when run as part of the complete suite."
	)
	void testQuerySkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session1) -> {
			session1.find(Book.class,1, PESSIMISTIC_WRITE);

			factoryScope.inTransaction( (session2) -> {
				final List<Book> books = session2.createQuery( "from Book", Book.class )
						.setLockMode( LockModeType.PESSIMISTIC_WRITE )
						.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, SKIP_LOCKED.milliseconds() )
						.getResultList();
				// id=1 should be skipped since it is locked in `session1`
				assertThat( books ).hasSize( 3 );
				assertThat( books ).map( Book::getId ).containsOnly( 2, 3, 4 );
			} );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@SkipForDialect(
			dialectClass = MariaDBDialect.class,
			reason = "Cannot figure this out - it passes when run by itself, but fails when run as part of the complete suite."
	)
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "no failure")
	void testFindSkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.find(Book.class,1, PESSIMISTIC_WRITE);

			factoryScope.inTransaction( (session2) -> {
				final Book book = session2.find( Book.class, 1, PESSIMISTIC_WRITE, SKIP_LOCKED );
				assertThat( book ).isNull();
			} );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void testLockSkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book book = session.find( Book.class, 1 );
			try {
				session.lock( book, PESSIMISTIC_WRITE, SKIP_LOCKED );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException iae) {
				assertThat( iae.getMessage() ).isEqualTo( ILLEGAL_SKIP_LOCKED );
			}
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWait.class)
	void testFindWait(SessionFactoryScope factoryScope) {
		AsyncExecutor.executeAsync( 1, TimeUnit.SECONDS, () -> {
			factoryScope.inTransaction( (session) -> {
				session.find(Book.class,1, PESSIMISTIC_WRITE);
			} );
		} );

		factoryScope.inTransaction( (session) -> {
			final Book book = session.find( Book.class, 1, PESSIMISTIC_WRITE, Timeout.seconds(3) );
			assertThat( book ).isNotNull();
		} );
	}
}
