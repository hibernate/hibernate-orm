/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name = JAKARTA_SHARED_CACHE_MODE, value = "ALL"),
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true")
})
@DomainModel(annotatedClasses = MultiLoadSecondLlvCacheTest.Event.class)
@SessionFactory(generateStatistics = true)
public class MultiLoadSecondLlvCacheTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		final PersistentClass entityBinding = modelScope.getEntityBinding( Event.class );
		assertTrue( entityBinding.getRootClass().isCached() );

		factoryScope.inTransaction( session -> {
			session.persist( new Event( 1, "text1" ) );
			session.persist( new Event( 2, "text2" ) );
			session.persist( new Event( 3, "text3" ) );
		} );

		factoryScope.getSessionFactory().getCache().evictEntityData( Event.class, 1 );
		var statistics = factoryScope.getSessionFactory().getStatistics();
		statistics.clear();

		factoryScope.inSession( session -> {
			var events = session.byMultipleIds( Event.class )
					.with(CacheMode.NORMAL)
					.with(LockMode.NONE)
					.multiLoad( List.of(1,2,3) );

			// check all elements are not null
			assertThat( events ).doesNotContainNull();
		} );

		assertThat( statistics.getEntityLoadCount() ).isOne();
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCachePutCount() ).isOne();
	}

	@Entity(name = "Event")
	@Cacheable
	public static class Event {

		@Id
		private Integer id;

		@Basic
		private String text;

		public Event() {
		}

		public Event(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}
}
