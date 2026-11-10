package org.hibernate.orm.test.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess.SoftLockImpl;
import org.hibernate.cache.spi.support.EntityReadWriteAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = PessimisticForceIncrementCacheTest.CachedEntity.class)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10"),
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.CACHE_REGION_FACTORY,
				value = "org.hibernate.testing.cache.CachingRegionFactory")
})
@SessionFactory
@JiraKey("HHH-20417")
public class PessimisticForceIncrementCacheTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	void testStaleLockReleasesCacheLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new CachedEntity( 1L ) ) );
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				final var entity = session.find( CachedEntity.class, 1L );
				// Change the database version without invalidating the cached entry.
				session.doWork( connection -> {
					try ( var statement = connection.prepareStatement(
							"update force_increment_cached set version = 1 where id = 1" ) ) {
						assertThat( statement.executeUpdate() ).isEqualTo( 1 );
					}
				} );
				assertThatThrownBy( () -> session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT ) )
						.isInstanceOf( StaleObjectStateException.class );
				assertThat( entity.version ).isZero();
			}
			finally {
				transaction.rollback();
			}
			final var persister = scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( CachedEntity.class );
			final var cache = (EntityReadWriteAccess) persister.getCacheAccessStrategy();
			final var cacheKey = cache.generateCacheKey( 1L, persister, scope.getSessionFactory(), null );
			final var cached = cache.getStorageAccess().getFromCache( cacheKey, session );
			assertThat( cached ).isInstanceOf( SoftLockImpl.class );
			// A newer version must be writable without relying on the soft-lock timeout.
			assertThat( ((SoftLockImpl) cached).isWriteable(
					Long.MIN_VALUE, 1L, (left, right) -> Long.compare( (Long) left, (Long) right ) ) ).isTrue();
		} );
	}

	@Entity(name = "CachedEntity")
	@Table(name = "force_increment_cached")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class CachedEntity {
		@Id
		private Long id;
		@Version
		private long version;

		public CachedEntity() {
		}

		CachedEntity(Long id) {
			this.id = id;
		}
	}
}
