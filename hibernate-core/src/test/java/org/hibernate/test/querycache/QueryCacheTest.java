package org.hibernate.test.querycache;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.querycache.CompositeKey;
import org.hibernate.orm.test.querycache.EntityWithCompositeKey;
import org.hibernate.orm.test.querycache.EntityWithStringCompositeKey;
import org.hibernate.orm.test.querycache.Item;
import org.hibernate.orm.test.querycache.StringCompositeKey;
import org.hibernate.stat.QueryStatistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/querycache/Item.hbm.xml",
		annotatedClasses = {
				CompositeKey.class,
				EntityWithCompositeKey.class,
				StringCompositeKey.class,
				EntityWithStringCompositeKey.class
		},
		concurrencyStrategy = "nonstrict-read-write"
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_PREFIX, value = "foo"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
public class QueryCacheTest {

	/*
	All the other tests of QueryCacheTest have been already moved into org.hibernate.orm test paxkage
	 */
	@Test
	public void testQueryCacheFetch(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();

		// persist our 2 items.  This saves them to the db, but also into the second level entity cache region
		scope.inTransaction(
				session -> {
					Item i = new Item();
					i.setName( "widget" );
					i.setDescription( "A really top-quality, full-featured widget." );
					Item i2 = new Item();
					i2.setName( "other widget" );
					i2.setDescription( "Another decent widget." );
					session.persist( i );
					session.persist( i2 );
				}
		);

		final String queryString = "from Item i where i.name like '%widget'";

		QueryStatistics qs = scope.getSessionFactory().getStatistics().getQueryStatistics( queryString );

		Thread.sleep( 200 );

		// perform the cacheable query.  this will execute the query (no query cache hit), but the Items will be
		// found in second level entity cache region
		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 2, result.size() );
				}
		);

		assertEquals( 0, qs.getCacheHitCount() );
		assertEquals( 0, scope.getSessionFactory().getStatistics().getEntityFetchCount() );

		// evict the Items from the second level entity cache region
		scope.getSessionFactory().getCache().evictEntityRegion( Item.class );

		// now, perform the cacheable query again.  this time we should not execute the query (query cache hit).
		// However, the Items will not be found in second level entity cache region this time (we evicted them above)
		// nor are they in associated with the session.
		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 2, result.size() );
					assertTrue( Hibernate.isInitialized( result.get( 0 ) ) );
					assertTrue( Hibernate.isInitialized( result.get( 1 ) ) );
				}
		);

		assertEquals( 1, qs.getCacheHitCount() );
		assertEquals( 1, scope.getSessionFactory().getStatistics().getEntityFetchCount() );

		scope.inTransaction(
				session ->
						session.createQuery( "delete Item" ).executeUpdate()
		);
	}

}
