/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.UPDATE_RETURNING;

/// Corollary to [OptimisticLockTest] using JPA's [ExcludedFromVersioning] instead of
/// Hibernate's [org.hibernate.annotations.OptimisticLock].
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = { ExcludedFromVersioningTests.Phone.class, ExcludedFromVersioningTests.Counter.class })
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
public class ExcludedFromVersioningTests {
	private static final Logger log = Logger.getLogger( ExcludedFromVersioningTests.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails at SERIALIZABLE isolation")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-7890");
			session.persist(phone);
		});

		factoryScope.inTransaction( session -> {
			var phone = session.find( Phone.class, 1L);
			phone.setNumber("+123-456-7890");

			factoryScope.inTransaction( _session -> {
				var _phone = _session.find( Phone.class, 1L);
				_phone.incrementCallCount();

				log.info("Bob changes the Phone call count");
			});

			log.info("Alice changes the Phone number");
		} );
		//end::locking-optimistic-exclude-attribute-example[]
	}

	@Test
	void testEntityAgentUpdateWithoutSnapshot(SessionFactoryScope factoryScope) {
		final var phone = factoryScope.fromTransaction( session -> {
			final var created = new Phone();
			created.setId( 1L );
			created.setNumber( "123-456-7890" );
			session.persist( created );
			return created;
		} );

		phone.incrementCallCount();
		factoryScope.getCollectingStatementObserver().clear();
		factoryScope.getSessionFactory()
				.runInTransaction( EntityAgent.class,
						agent -> agent.update( phone ) );

		assertVersionSelect( factoryScope );
		assertThat( phone.getVersion() ).isZero();
		factoryScope.inTransaction( session -> {
			final var reloaded = session.find( Phone.class, phone.getId() );
			assertThat( reloaded.getCallCount() ).isEqualTo( 1 );
			assertThat( reloaded.getVersion() ).isZero();
		} );

		phone.setNumber( "+123-456-7890" );
		factoryScope.getCollectingStatementObserver().clear();
		factoryScope.getSessionFactory()
				.runInTransaction( EntityAgent.class,
						agent -> agent.update( phone ) );

		assertVersionSelect( factoryScope );
		assertThat( phone.getVersion() ).isOne();
		factoryScope.inTransaction( session -> {
			final var reloaded = session.find( Phone.class, phone.getId() );
			assertThat( reloaded.getNumber() ).isEqualTo( "+123-456-7890" );
			assertThat( reloaded.getVersion() ).isOne();
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testEntityAgentUpsertWithExcludedProperties(boolean multiple, SessionFactoryScope factoryScope) {
		final var phone = factoryScope.fromTransaction( session -> {
			final var created = new Phone();
			created.setId( 1L );
			created.setNumber( "123-456-7890" );
			session.persist( created );
			return created;
		} );

		for ( int i = 0; i < 3; i++ ) {
			phone.incrementCallCount();
			if ( i == 1 ) {
				phone.setNumber( "+123-456-7890" );
			}
			final long expectedVersion = i == 0 ? 0 : 1;
			factoryScope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> {
				if ( multiple ) {
					agent.upsertMultiple( List.of( phone ) );
				}
				else {
					agent.upsert( phone );
				}
				assertThat( phone.getVersion() ).isEqualTo( expectedVersion );
			} );
			factoryScope.inTransaction( session -> {
				final var reloaded = session.find( Phone.class, phone.getId() );
				assertThat( reloaded.getCallCount() ).isEqualTo( phone.getCallCount() );
				assertThat( reloaded.getNumber() ).isEqualTo( phone.getNumber() );
				assertThat( reloaded.getVersion() ).isEqualTo( expectedVersion );
			} );
		}
	}

	@Test @JiraKey("HHH-20828")
	void testEntityAgentUpdateWithOnlyExcludedProperties(SessionFactoryScope factoryScope) {
		final var counter = factoryScope.fromTransaction( session -> {
			final var created = new Counter();
			created.id = 1L;
			session.persist( created );
			return created;
		} );

		counter.excludedValue++;
		factoryScope.getCollectingStatementObserver().clear();
		factoryScope.getSessionFactory()
				.runInTransaction( EntityAgent.class,
						agent -> agent.update( counter ) );

		assertVersionSelect( factoryScope );
		assertThat( counter.version ).isZero();
		factoryScope.inTransaction( session -> {
			final var reloaded = session.find( Counter.class, counter.id );
			assertThat( reloaded.excludedValue ).isOne();
			assertThat( reloaded.version ).isZero();
		} );
	}

	@Test @JiraKey("HHH-20828")
	void testEntityAgentUpdateWithoutChanges(SessionFactoryScope factoryScope) {
		final var phone = factoryScope.fromTransaction( session -> {
			final var created = new Phone();
			created.setId( 1L );
			session.persist( created );
			return created;
		} );

		factoryScope.getCollectingStatementObserver().clear();
		factoryScope.getSessionFactory()
				.runInTransaction( EntityAgent.class, agent -> agent.update( phone ) );

		assertVersionSelect( factoryScope );
		assertThat( phone.getVersion() ).isZero();
		factoryScope.inTransaction( session ->
				assertThat( session.find( Phone.class, phone.getId() ).getVersion() ).isZero() );
	}

	private static void assertVersionSelect(SessionFactoryScope factoryScope) {
		if ( !factoryScope.getSessionFactory().getJdbcServices().getDialect()
				.getGeneratedValuesSupport().supports( UPDATE_RETURNING ) ) {
			final var observer = factoryScope.getCollectingStatementObserver();
			observer.assertQueries().hasSize( 2 );
			observer.assertQuery( 0 ).startsWith( "update " );
			observer.assertQuery( 1 ).startsWith( "select version as version_ from " );
		}
	}

	@Entity(name = "Phone")
	public static class Phone {
		@Id
		private Long id;
		@Column(name = "`number`")
		private String number;

		@ExcludedFromVersioning
		private long callCount;

		@Version
		private Long version;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Long getVersion() {
			return version;
		}

		public long getCallCount() {
			return callCount;
		}

		public void incrementCallCount() {
			this.callCount++;
		}
	}

	@Entity(name = "Counter")
	public static class Counter {
		@Id
		private Long id;

		@ExcludedFromVersioning
		private long excludedValue;

		@Version
		private Long version;
	}
}
