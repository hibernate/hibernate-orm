package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that typed follow-on options reach lock and refresh processing.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnOptionTests.Item.class)
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FollowOnOptionTests {

	private Locking.FollowOn observedPolicy;
	private SessionFactoryImplementor observedFactory;

	private void observeOptions(SessionFactoryScope scope) {
		if ( observedFactory == scope.getSessionFactory() ) {
			return;
		}
		observedFactory = scope.getSessionFactory();
		var registry = scope.getSessionFactory().getEventListenerRegistry();
		registry.prependListeners( EventType.LOCK,
				(LockEventListener) event -> observedPolicy = event.getLockOptions().getFollowOnStrategy() );
		registry.prependListeners( EventType.REFRESH, new RefreshEventListener() {
			@Override
			public void onRefresh(RefreshEvent event) {
				observedPolicy = event.getLockOptions().getFollowOnStrategy();
			}

			@Override
			public void onRefresh(RefreshEvent event, RefreshContext refreshedAlready) {
				onRefresh( event );
			}
		} );
	}

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		observeOptions( scope );
		observedPolicy = null;
		scope.inTransaction( session -> session.persist( new Item( 1L, "Original" ) ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void standardLock(Locking.FollowOn policy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			Item item = entityManager.find( Item.class, 1L );
			entityManager.lock( item, LockModeType.PESSIMISTIC_WRITE, policy );
			assertThat( observedPolicy ).isEqualTo( policy );
			assertThat( entityManager.getLockMode( item ) ).isEqualTo( LockModeType.PESSIMISTIC_WRITE );
		} );
	}

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void nativeLock(Locking.FollowOn policy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Item item = session.find( Item.class, 1L );
			session.lock( item, LockMode.PESSIMISTIC_WRITE, policy );
			assertThat( observedPolicy ).isEqualTo( policy );
			assertThat( session.getLockMode( item ) ).isEqualTo( LockModeType.PESSIMISTIC_WRITE );
		} );
	}

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void standardRefresh(Locking.FollowOn policy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			Item item = entityManager.find( Item.class, 1L );
			item.name = "Unflushed";
			scope.getCollectingStatementObserver().clear();
			entityManager.refresh( item, LockModeType.PESSIMISTIC_WRITE, policy );
			assertRefresh( item, policy, scope );
			assertThat( entityManager.getLockMode( item ) ).isEqualTo( LockModeType.PESSIMISTIC_WRITE );
		} );
	}

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void nativeRefresh(Locking.FollowOn policy, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Item item = session.find( Item.class, 1L );
			item.name = "Unflushed";
			scope.getCollectingStatementObserver().clear();
			session.refresh( item, LockMode.PESSIMISTIC_WRITE, policy );
			assertRefresh( item, policy, scope );
			assertThat( session.getLockMode( item ) ).isEqualTo( LockModeType.PESSIMISTIC_WRITE );
		} );
	}

	private void assertRefresh(Item item, Locking.FollowOn policy, SessionFactoryScope scope) {
		assertThat( observedPolicy ).isEqualTo( policy );
		assertThat( item.name ).isEqualTo( "Original" );
		var style = scope.getSessionFactory().getJdbcServices().getDialect()
				.getLockingSupport().getMetadata().getPessimisticLockStyle();
		int expectedStatements = policy == Locking.FollowOn.FORCE && style != PessimisticLockStyle.TABLE_HINT ? 2 : 1;
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).hasSize( expectedStatements );
	}

	@Entity(name = "FollowOnOptionItem")
	static class Item {
		@Id
		private Long id;
		private String name;

		Item() {
		}

		Item(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
