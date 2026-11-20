package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;

import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		PessimisticForceIncrementBatchingTest.VersionedEntity.class,
		PessimisticForceIncrementBatchingTest.JoinedEntity.class,
		PessimisticForceIncrementBatchingTest.JoinedChild.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10"))
@SessionFactory
@JiraKey("HHH-20417")
public class PessimisticForceIncrementBatchingTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new VersionedEntity( 1L ) ) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testManagedLockExecutesImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1L );
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.version ).isEqualTo( 1L );
			// Read on the same connection without triggering an automatic flush.
			final long databaseVersion = session.doReturningWork( connection -> {
				try ( var statement = connection.prepareStatement(
						"select version from force_increment_entity where id = ?" ) ) {
					statement.setLong( 1, entity.id );
					try ( var resultSet = statement.executeQuery() ) {
						assertThat( resultSet.next() ).isTrue();
						return resultSet.getLong( 1 );
					}
				}
			} );
			assertThat( databaseVersion ).isEqualTo( 1L );
		} );
	}

	@Test
	void testFindExecutesImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1L, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.version ).isEqualTo( 1L );
			// Read on the same connection without triggering an automatic flush.
			final long databaseVersion = session.doReturningWork( connection -> {
				try ( var statement = connection.prepareStatement(
						"select version from force_increment_entity where id = ?" ) ) {
					statement.setLong( 1, entity.id );
					try ( var resultSet = statement.executeQuery() ) {
						assertThat( resultSet.next() ).isTrue();
						return resultSet.getLong( 1 );
					}
				}
			} );
			assertThat( databaseVersion ).isEqualTo( 1L );
		} );
	}

	@Test
	void testJoinedSubclassLockExecutesImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new JoinedChild( 2L ) ) );
		scope.inTransaction( session -> {
			final JoinedEntity entity = session.find( JoinedChild.class, 2L );
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.version ).isEqualTo( 1L );
			final long databaseVersion = session.doReturningWork( connection -> {
				try ( var statement = connection.prepareStatement(
						"select version from force_increment_joined where id = ?" ) ) {
					statement.setLong( 1, entity.id );
					try ( var resultSet = statement.executeQuery() ) {
						assertThat( resultSet.next() ).isTrue();
						return resultSet.getLong( 1 );
					}
				}
			} );
			assertThat( databaseVersion ).isEqualTo( 1L );
		} );
	}

	@Test
	void testSessionBatchSizeOverride(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 20 );
			final var entity = session.find( VersionedEntity.class, 1L );
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	@Test
	void testBatchingDisabled(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 0 );
			final var entity = session.find( VersionedEntity.class, 1L );
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	@Test
	void testRefreshExecutesImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1L );
			session.refresh( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.version ).isEqualTo( 1L );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
	void testFollowOnLockExecutesImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = session.createSelectionQuery( "from VersionedEntity", VersionedEntity.class )
					.setLockMode( LockModeType.PESSIMISTIC_FORCE_INCREMENT )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.getSingleResult();
			assertThat( entity.version ).isEqualTo( 1L );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	@Test
	void testOptimisticIncrementRemainsDeferred(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1L );
			session.lock( entity, LockMode.OPTIMISTIC_FORCE_INCREMENT );
			assertThat( entity.version ).isZero();
			assertThat( databaseVersion( session ) ).isZero();
			session.flush();
			assertThat( entity.version ).isZero();
			assertThat( databaseVersion( session ) ).isZero();
		} );
		scope.inTransaction( session -> assertThat( databaseVersion( session ) ).isEqualTo( 1L ) );
	}

	@Test
	void testOrdinaryUpdatesStillUseBatching(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new VersionedEntity( 2L ) ) );
		final var batches = new AtomicInteger();
		scope.inTransaction( session -> {
			session.addEventListeners( new SessionEventListener() {
				@Override
				public void jdbcExecuteBatchStart() {
					batches.incrementAndGet();
				}
			} );
			final var first = session.find( VersionedEntity.class, 1L );
			final var second = session.find( VersionedEntity.class, 2L );
			session.lock( first, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( batches.get() ).isZero();
			first.name = "updated first";
			second.name = "updated second";
			session.flush();
			assertThat( batches.get() ).isEqualTo( 1 );
		} );
	}

	@Test
	void testPendingBatchExecutesBeforeLockReturns(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new VersionedEntity( 2L ) ) );
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 2L );
			final var persister = scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( VersionedEntity.class );
			// Queue a real version mutation without a flush, which would drain the batch.
			persister.forceVersionIncrement( 1L, 0L, true, session );
			assertThat( databaseVersion( session ) ).isZero();
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.version ).isEqualTo( 1L );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	private static long databaseVersion(Session session) {
		// Direct JDBC avoids any automatic flush before observing the version.
		return session.doReturningWork( connection -> {
			try ( var statement = connection.prepareStatement(
					"select version from force_increment_entity where id = 1" );
					var resultSet = statement.executeQuery() ) {
				assertThat( resultSet.next() ).isTrue();
				return resultSet.getLong( 1 );
			}
		} );
	}

	@Entity(name = "VersionedEntity")
	@Table(name = "force_increment_entity")
	public static class VersionedEntity {
		@Id
		private Long id;
		@Version
		private long version;
		private String name;

		public VersionedEntity() {
		}

		VersionedEntity(Long id) {
			this.id = id;
		}
	}
	@Entity(name = "JoinedEntity")
	@Table(name = "force_increment_joined")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class JoinedEntity {
		@Id
		private Long id;
		@Version
		private long version;

		public JoinedEntity() {
		}

		JoinedEntity(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "JoinedChild")
	@Table(name = "force_increment_child")
	public static class JoinedChild extends JoinedEntity {
		public JoinedChild() {
		}

		JoinedChild(Long id) {
			super( id );
		}
	}

}
