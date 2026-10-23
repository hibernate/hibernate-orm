package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.Timeouts.SKIP_LOCKED;

/// Reproduces follow-on acquisition returning a row skipped because another
/// transaction holds its lock. The direct query is a control demonstrating
/// that the conflicting lock is held and H2 skips the row correctly.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnSkipLockedTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21028" )
class FollowOnSkipLockedTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1, "available" ) );
			session.persist( new Item( 2, "locked elsewhere" ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void directAcquisitionSkipsLockedEntity(SessionFactoryScope scope) {
		scope.inTransaction( holder -> {
			holder.find( Item.class, 2, LockModeType.PESSIMISTIC_WRITE );
			scope.inTransaction( reader -> {
				final var items = reader.createQuery( "from SkipLockedItem order by id", Item.class )
						.setLockMode( LockModeType.PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.setLockTimeout( SKIP_LOCKED )
						.getResultList();
				assertThat( items ).extracting( item -> item.id ).containsExactly( 1 );
			} );
		} );
	}

	@Test
	void nativeDirectSkipLockedRetainsPaginationAndResults(SessionFactoryScope scope) {
		final List<String> statements = new ArrayList<>();
		final UnaryOperator<String> inspector = sql -> { statements.add( sql ); return sql; };
		scope.inTransaction( holder -> {
			holder.find( Item.class, 2, LockModeType.PESSIMISTIC_WRITE );
			try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
				final var transaction = reader.beginTransaction();
				try {
					final var items = reader.createNativeQuery( "select * from follow_on_skip_locked_item order by id", Item.class )
							.setHibernateLockMode( LockMode.UPGRADE_SKIPLOCKED )
							.setMaxResults( 10 ).getResultList();
					assertThat( items ).extracting( item -> item.id ).containsExactly( 1 );
				}
				finally {
					transaction.rollback();
				}
			}
		} );
		scope.inTransaction( session -> {
			final var items = session.createNativeQuery( "select * from follow_on_skip_locked_item order by id", Item.class )
					.setHibernateLockMode( LockMode.UPGRADE_SKIPLOCKED )
					.setFirstResult( 1 ).setMaxResults( 1 ).getResultList();
			assertThat( items ).extracting( item -> item.id ).containsExactly( 2 );
		} );
		assertThat( statements ).singleElement().asString().contains( "skip locked", "?" );
	}

	@Test
	void unsupportedFollowOnSkipLockedIsRejectedBeforeExecution(SessionFactoryScope scope) {
		scope.inTransaction( holder -> {
			holder.find( Item.class, 2, LockModeType.PESSIMISTIC_WRITE );
			assertRejected( scope, Locking.FollowOn.FORCE, false, false );
			assertRejected( scope, Locking.FollowOn.ALLOW, false, false );
		} );
	}

	private void assertRejected(SessionFactoryScope scope, Locking.FollowOn strategy, boolean legacy, boolean empty) {
		final List<String> statements = new ArrayList<>();
		final UnaryOperator<String> inspector = sql -> { statements.add( sql ); return sql; };
		try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var transaction = reader.beginTransaction();
			try {
				final var query = query( reader, strategy, empty );
				if ( legacy ) {
					query.setHibernateLockMode( LockMode.UPGRADE_SKIPLOCKED );
				}
				else {
					query.setLockTimeout( SKIP_LOCKED );
				}
				assertUnsupported( query::getResultList );
				assertThat( statements ).isEmpty();
			}
			finally {
				transaction.rollback();
			}
		}
	}

	@Test
	void cachedQueryCanSwitchBetweenOrdinaryFollowOnAndDirectSkipLocked(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = query( session, Locking.FollowOn.FORCE, false );
			assertThat( query.getResultList() ).hasSize( 2 );
			query.setLockTimeout( SKIP_LOCKED );
			assertUnsupported( query::getResultList );
			query.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			assertThat( query.getResultList() ).hasSize( 2 );
			query.setFollowOnStrategy( Locking.FollowOn.FORCE ).setLockTimeout( Timeouts.WAIT_FOREVER );
			assertThat( query.getResultList() ).hasSize( 2 );
		} );
	}

	private SelectionQuery<Item> query(org.hibernate.Session session, Locking.FollowOn strategy, boolean empty) {
		// DISTINCT requires automatic follow-on locking on H2.
		return session.createQuery(
				(strategy == Locking.FollowOn.ALLOW ? "select distinct i from SkipLockedItem i" : "from SkipLockedItem i")
						+ (empty ? " where i.id < 0" : "") + " order by i.id", Item.class )
				.setLockMode( LockModeType.PESSIMISTIC_WRITE ).setFollowOnStrategy( strategy );
	}

	private void assertUnsupported(Runnable work) {
		assertThatThrownBy( work::run ).isInstanceOf( IllegalStateException.class )
				.hasCauseInstanceOf( IllegalQueryOperationException.class )
				.hasMessageContaining( "SKIP_LOCKED is not supported with follow-on locking" );
	}

	@Entity(name = "SkipLockedItem")
	@Table(name = "follow_on_skip_locked_item")
	static class Item {
		@Id
		int id;
		String title;

		Item() {
		}

		Item(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
