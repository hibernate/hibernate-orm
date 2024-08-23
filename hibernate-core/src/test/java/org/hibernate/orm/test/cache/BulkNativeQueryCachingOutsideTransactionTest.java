/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cache;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.AbstractEntityDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.cache.MapStorageAccessImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests of how sync-spaces for a native query affect caching
 *
 * @author Samuel Fung
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				BulkNativeQueryCachingOutsideTransactionTest.Customer.class
		}
)
@SessionFactory()
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "true"),
		},
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.CACHE_REGION_FACTORY,
				provider = BulkNativeQueryCachingOutsideTransactionTest.CacheRegionFactoryProvider.class
		)
)

public class BulkNativeQueryCachingOutsideTransactionTest {

	private static final TestCachingRegionFactory CACHING_REGION_FACTORY = new TestCachingRegionFactory();

	public static class CacheRegionFactoryProvider implements SettingProvider.Provider<TestCachingRegionFactory> {
		@Override
		public TestCachingRegionFactory getSetting() {
			return CACHING_REGION_FACTORY;
		}
	}

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.persist( new Customer( 1, "Samuel" ) )
		);
		CACHING_REGION_FACTORY.getTestDomainDataRegion().getTestEntityDataAccess().reset();
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete Customer" ).executeUpdate()
		);
		CACHING_REGION_FACTORY.getTestDomainDataRegion().getTestEntityDataAccess().reset();
	}

	@Test
	public void testUpdateCachedEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createNativeQuery( "update Customer set id = id" ).executeUpdate()
		);
		assertThat( isLockRegionCalled() )
				.as( "EntityDataAccess lockRegion method has not been called" )
				.isTrue();

		assertThat( isUnlockRegionCalled() )
				.as( "EntityDataAccess unlockRegion method has not been called" )
				.isTrue();
	}

	@Test
	public void testUpdateCachedOutsideTransaction(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.createNativeQuery( "update Customer set id = id" ).executeUpdate()
		);

		assertThat( isLockRegionCalled() )
				.as( "EntityDataAccess lockRegion method has not been called" )
				.isTrue();

		assertThat( isUnlockRegionCalled() )
				.as( "EntityDataAccess unlockRegion method has not been called" )
				.isTrue();
	}

	private static boolean isUnlockRegionCalled() {
		return CACHING_REGION_FACTORY.getTestDomainDataRegion()
				.getTestEntityDataAccess()
				.isUnlockRegionCalled();
	}

	private static boolean isLockRegionCalled() {
		return CACHING_REGION_FACTORY.getTestDomainDataRegion()
				.getTestEntityDataAccess()
				.isLockRegionCalled();
	}

	@Entity(name = "Customer")
	@Table(name = "Customer")
	@Cacheable
	public static class Customer {
		@Id
		private int id;

		private String name;

		public Customer() {
		}

		public Customer(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class TestCachingRegionFactory extends CachingRegionFactory {
		private static final Logger LOG = Logger.getLogger( org.hibernate.testing.cache.CachingRegionFactory.class.getName() );

		public static final String DEFAULT_ACCESSTYPE = "DefaultAccessType";
		private final CacheKeysFactory cacheKeysFactory;
		private TestDomainDataRegion testDomainDataRegion;

		public TestCachingRegionFactory() {
			this( DefaultCacheKeysFactory.INSTANCE, null );
		}

		public TestCachingRegionFactory(CacheKeysFactory cacheKeysFactory) {
			this( cacheKeysFactory, null );
		}

		public TestCachingRegionFactory(Properties properties) {
			this( DefaultCacheKeysFactory.INSTANCE, properties );
		}

		public TestCachingRegionFactory(CacheKeysFactory cacheKeysFactory, Properties properties) {
			LOG.warn( "org.hibernate.testing.cache.CachingRegionFactory should be only used for testing." );
			this.cacheKeysFactory = cacheKeysFactory;
		}

		@Override
		public DomainDataRegion buildDomainDataRegion(
				DomainDataRegionConfig regionConfig, DomainDataRegionBuildingContext buildingContext) {
			if ( testDomainDataRegion == null ) {
				testDomainDataRegion = new TestDomainDataRegion(
						regionConfig,
						this,
						new MapStorageAccessImpl(),
						cacheKeysFactory,
						buildingContext
				);
			}
			return testDomainDataRegion;
		}

		@Override
		protected void releaseFromUse() {
			testDomainDataRegion = null;
		}

		public TestDomainDataRegion getTestDomainDataRegion() {
			return testDomainDataRegion;
		}
	}

	public static class TestDomainDataRegion extends DomainDataRegionImpl {

		TestEntityDataAccess testEntityDataAccess;

		public TestDomainDataRegion(
				DomainDataRegionConfig regionConfig,
				RegionFactoryTemplate regionFactory,
				DomainDataStorageAccess domainDataStorageAccess,
				CacheKeysFactory defaultKeysFactory,
				DomainDataRegionBuildingContext buildingContext) {
			super( regionConfig, regionFactory, domainDataStorageAccess, defaultKeysFactory, buildingContext );
		}

		@Override
		public EntityDataAccess generateEntityAccess(EntityDataCachingConfig entityAccessConfig) {
			if ( testEntityDataAccess == null ) {
				testEntityDataAccess = new TestEntityDataAccess(
						this,
						getEffectiveKeysFactory(),
						getCacheStorageAccess()
				);
			}
			return testEntityDataAccess;
		}

		public TestEntityDataAccess getTestEntityDataAccess() {
			return testEntityDataAccess;
		}

		@Override
		public void destroy() throws CacheException {
			testEntityDataAccess = null;
		}
	}

	public static class TestEntityDataAccess extends AbstractEntityDataAccess {

		private boolean isUnlockRegionCalled = false;
		private boolean lockRegionCalled = false;

		public TestEntityDataAccess(
				DomainDataRegion region,
				CacheKeysFactory cacheKeysFactory,
				DomainDataStorageAccess storageAccess) {
			super( region, cacheKeysFactory, storageAccess );
		}

		@Override
		public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
			return false;
		}

		@Override
		public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
			return false;
		}

		@Override
		public boolean update(
				SharedSessionContractImplementor session,
				Object key,
				Object value,
				Object currentVersion,
				Object previousVersion) {
			return false;
		}

		@Override
		public boolean afterUpdate(
				SharedSessionContractImplementor session,
				Object key,
				Object value,
				Object currentVersion,
				Object previousVersion,
				SoftLock lock) {
			return false;
		}

		@Override
		public AccessType getAccessType() {
			return null;
		}

		@Override
		public SoftLock lockRegion() {
			lockRegionCalled = true;
			return super.lockRegion();
		}

		@Override
		public void unlockRegion(SoftLock lock) {
			super.unlockRegion( lock );
			isUnlockRegionCalled = true;
		}

		@Override
		public void destroy() {
			super.destroy();
			isUnlockRegionCalled = false;
		}

		public boolean isUnlockRegionCalled() {
			return isUnlockRegionCalled;
		}

		public boolean isLockRegionCalled() {
			return lockRegionCalled;
		}

		public void reset() {
			this.isUnlockRegionCalled = false;
			this.lockRegionCalled = false;
		}

	}


}
