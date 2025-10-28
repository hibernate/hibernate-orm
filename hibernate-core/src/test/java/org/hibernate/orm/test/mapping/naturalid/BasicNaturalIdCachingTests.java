/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.NaturalIdStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@DomainModel( annotatedClasses = BasicNaturalIdCachingTests.CachedEntity.class )
@SessionFactory
public class BasicNaturalIdCachingTests {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		final NaturalIdDataAccess cacheAccess = resolveCacheAccess( scope );
		assertThat( cacheAccess, notNullValue() );
	}

	private NaturalIdDataAccess resolveCacheAccess(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( CachedEntity.class );
		return entityPersister.getNaturalIdMapping().getCacheAccess();
	}

	@Test
	public void testCreationCaching(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final StatisticsImplementor statistics = sessionFactory.getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					session.persist( new CachedEntity( 1, "abc", "the entity" ) );
				}
		);

		final NaturalIdStatistics cachedStats = statistics.getNaturalIdStatistics( CachedEntity.class.getName() );
		assertThat( cachedStats, notNullValue() );
		assertThat( cachedStats.getCacheHitCount(), is( 0L ) );
		assertThat( cachedStats.getCacheMissCount(), is( 0L ) );
		assertThat( cachedStats.getCachePutCount(), is( 1L ) );

		scope.inTransaction(
				(session) -> {
					final EntityPersister entityPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( CachedEntity.class );
					final NaturalIdDataAccess cacheAccess = resolveCacheAccess( scope );
					final Object cacheKey = cacheAccess.generateCacheKey( "abc", entityPersister, session );
					final Object cached = cacheAccess.get( session, cacheKey );
					assertThat( cached, notNullValue() );
					assertThat( cached, equalTo( 1 ) );
				}
		);

	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();

		// make sure the data is not in the L2 cache
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.getSessionFactory().getCache().evictNaturalIdData();
	}

	@Entity( name = "CachedEntity" )
	@Table( name = "natural_id_cached" )
	@NaturalIdCache
	public static class CachedEntity {
		@Id
		private Integer id;
		@NaturalId
		private String code;
		private String name;

		public CachedEntity() {
		}

		public CachedEntity(Integer id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
