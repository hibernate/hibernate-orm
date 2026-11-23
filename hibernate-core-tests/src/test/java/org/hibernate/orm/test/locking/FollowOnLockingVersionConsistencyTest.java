package org.hibernate.orm.test.locking;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/// Reproduces a version change between initial selection and follow-on acquisition.
/// The second select is intercepted before preparation, after the initial result
/// has been materialized. A separate transaction commits its update before the
/// locking select executes, without sleeps or competing lock waits.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnLockingVersionConsistencyTest.Item.class)
@SessionFactory
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21029" )
class FollowOnLockingVersionConsistencyTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item( 1, "initial" ) ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void acquiredVersionMatchesEntityEntry(SessionFactoryScope scope) {
		withConcurrentUpdate( scope, (session, item) -> {
			final var entry = ((SharedSessionContractImplementor) session)
					.getPersistenceContextInternal().getEntry( item );
			assertThat( item.version ).isEqualTo( 1 );
			assertThat( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] )
					.as( "version in the loaded snapshot" ).isEqualTo( item.version );
			assertThat( entry.getVersion() )
					.as( "entity entry version after follow-on acquisition" ).isEqualTo( item.version );
		} );
	}

	@Test
	void acquiredEntityCanBeUpdated(SessionFactoryScope scope) {
		withConcurrentUpdate( scope, (session, item) -> {
			item.title = "local change";
			session.flush();
		} );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertThat( item.title ).isEqualTo( "local change" );
			assertThat( item.version ).isEqualTo( 2 );
		} );
	}

	private void withConcurrentUpdate(SessionFactoryScope scope, BiConsumer<Session, Item> assertion) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		final var selectCount = new AtomicInteger();
		final UnaryOperator<String> inspector = sql -> {
			if ( sql.startsWith( "select " ) && sql.contains( "follow_on_version_item" )
					&& selectCount.incrementAndGet() == 2 ) {
				// This session-specific inspector does not intercept the writer's SQL.
				scope.inTransaction( writer -> {
					FollowOnLockingTestSupport.assertReadCommitted( writer );
					final var item = writer.find( Item.class, 1 );
					assertThat( item.version ).isZero();
					item.title = "concurrent change";
				} );
			}
			return sql;
		};
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				final var query = session.createQuery( "from FollowOnVersionItem where id = 1", Item.class )
						.setLockMode( LockModeType.PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.FORCE );
				FollowOnLockingTestSupport.assertFollowOnPlan( (SharedSessionContractImplementor) session, query );
				final var item = query.getSingleResult();
				assertThat( selectCount.get() ).isEqualTo( 2 );
				assertThat( item.title ).isEqualTo( "concurrent change" );
				assertThat( item.version ).isEqualTo( 1 );
				assertion.accept( session, item );
				transaction.commit();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	@Entity(name = "FollowOnVersionItem")
	@Table(name = "follow_on_version_item")
	static class Item {
		@Id
		int id;
		@Version
		int version;
		String title;

		Item() {
		}

		Item(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
