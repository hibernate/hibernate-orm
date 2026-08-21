/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.CacheMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = ReadWriteMinimalPutsTest.CacheableItem.class)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_MINIMAL_PUTS, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY,
						value = "org.hibernate.testing.cache.CachingRegionFactory")
		}
)
class ReadWriteMinimalPutsTest {

	@Test
	void testMinimalPutsAreApplied(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( session -> session.persist( new CacheableItem( 1L, "one" ) ) );

		scope.inTransaction( session -> {
			session.find( CacheableItem.class, 1L, CacheMode.PUT );
		} );

		assertEquals( 1, statistics.getSecondLevelCachePutCount() );

		scope.inTransaction( session -> {
			session.find( CacheableItem.class, 1L, CacheMode.PUT );
		} );

		assertEquals( 1, statistics.getSecondLevelCachePutCount() );

		scope.inTransaction( session -> {
			session.find( CacheableItem.class, 1L, CacheMode.IGNORE );
		} );

		scope.inTransaction( session -> {
			session.find( CacheableItem.class, 1L, CacheMode.NORMAL );
		} );

		scope.inTransaction( session -> {
			session.find( CacheableItem.class, 1L, CacheMode.GET );
		} );

		assertEquals( 1, statistics.getSecondLevelCachePutCount() );

		scope.inTransaction( session -> {
			// we actually don't refresh, since we know it's not stale (via version)
			session.find( CacheableItem.class, 1L, CacheMode.REFRESH );
		} );

		assertEquals( 1, statistics.getSecondLevelCachePutCount() );

		scope.inTransaction( session -> {
			// sneaky update
			session.runWithConnection( (Connection connection) ->
					connection.prepareStatement(
							"UPDATE CacheableItem SET name = 'two', version=version+1 WHERE id = 1" )
							.executeUpdate() );
		} );

		scope.inTransaction( session -> {
			session.find( CacheableItem.class, 1L, CacheMode.NORMAL );
		} );

		assertEquals( 1, statistics.getSecondLevelCachePutCount() );

		scope.inTransaction( session -> {
			// this time there is something to refresh!
			session.find( CacheableItem.class, 1L, CacheMode.REFRESH );
		} );

		assertEquals( 2, statistics.getSecondLevelCachePutCount() );

	}

	@Entity(name = "CacheableItem")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class CacheableItem {
		@Id
		private Long id;
		@Version
		private Integer version;
		private String name;

		public CacheableItem() {
		}

		public CacheableItem(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
