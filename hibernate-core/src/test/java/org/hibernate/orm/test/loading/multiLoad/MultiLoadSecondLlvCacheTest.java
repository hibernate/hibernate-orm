/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiLoadSecondLlvCacheTest extends BaseCoreFunctionalTestCase {

	private Statistics statistics;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration
				.setProperty( AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.ALL )
				.setProperty( AvailableSettings.GENERATE_STATISTICS, true )
				.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
	}

	@Test
	public void test() {
		inTransaction( session -> {
			session.persist( new Event( 1, "text1" ) );
			session.persist( new Event( 2, "text2" ) );
			session.persist( new Event( 3, "text3" ) );
		} );

		sessionFactory().getCache().evictEntityData( Event.class, 1 );

		statistics = sessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();

		inSession( session -> {
			List<Event> events = session.byMultipleIds( Event.class )
					.with( CacheMode.NORMAL )
					.with( LockOptions.NONE )
					.multiLoad( 1, 2, 3 );

			// check all elements are not null
			assertThat( events ).filteredOn(item -> item != null).hasSize( 3 );
		} );

		assertThat( statistics.getEntityLoadCount() ).isOne();
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCachePutCount() ).isOne();
	}

	@Entity(name = "Event")
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
