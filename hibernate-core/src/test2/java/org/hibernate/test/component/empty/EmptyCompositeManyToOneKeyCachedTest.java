/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import java.io.Serializable;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SharedCacheMode;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EmptyCompositeManyToOneKeyCachedTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				OtherEntity.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, "true" );
		configuration.getProperties().put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		configuration.getProperties().put( Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, AccessType.READ_WRITE.getExternalName() );
		configuration.getProperties().put( Environment.USE_QUERY_CACHE, "true" );
		configuration.getProperties().put( Environment.GENERATE_STATISTICS, "true" );
		configuration.getProperties().put( Environment.CACHE_REGION_PREFIX, "" );
		configuration.getProperties().put( "javax.persistence.sharedCache.mode", SharedCacheMode.ALL );
	}

	@Test
	public void testGetEntityWithNullManyToOne() {

		sessionFactory().getCache().evictAllRegions();
		sessionFactory().getStatistics().clear();

		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		sessionFactory().getStatistics().clear();

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.find( AnEntity.class, id );
					assertNotNull( anEntity );
					assertNull( anEntity.otherEntity );
				}
		);

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );
	}

	@Test
	public void testQueryEntityWithNullManyToOne() {

		sessionFactory().getCache().evictAllRegions();
		sessionFactory().getStatistics().clear();

		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		sessionFactory().getStatistics().clear();

		final String queryString = "from AnEntity where id = " + id;

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							queryString,
							AnEntity.class
					).setCacheable( true ).uniqueResult();
					assertNull( anEntity.otherEntity );
				}
		);

		assertEquals( 0, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );

		sessionFactory().getStatistics().clear();

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							queryString,
							AnEntity.class
					).setCacheable( true ).uniqueResult();
					assertNull( anEntity.otherEntity );
				}
		);

		assertEquals( 1, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );
	}

	@Test
	public void testQueryEntityJoinFetchNullManyToOne() {

		sessionFactory().getCache().evictAllRegions();
		sessionFactory().getStatistics().clear();

		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		sessionFactory().getStatistics().clear();

		final String queryString = "from AnEntity e join fetch e.otherEntity where e.id = " + id;
		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							queryString,
							AnEntity.class
					).setCacheable( true ).uniqueResult();
					assertNull( anEntity );
				}
		);

		assertEquals( 0, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );

		sessionFactory().getStatistics().clear();

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							queryString,
							AnEntity.class
					).setCacheable( true ).uniqueResult();
					assertNull( anEntity );
				}
		);

		assertEquals( 1, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );
	}

	@Test
	public void testQueryEntityLeftJoinFetchNullManyToOne() {

		sessionFactory().getCache().evictAllRegions();
		sessionFactory().getStatistics().clear();

		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		sessionFactory().getStatistics().clear();

		final String queryString = "from AnEntity e left join fetch e.otherEntity where e.id = " + id;

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							queryString,
							AnEntity.class
					).setCacheable( true ).uniqueResult();
					assertNull( anEntity.otherEntity );
				}
		);

		assertEquals( 0, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );

		sessionFactory().getStatistics().clear();

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							queryString,
							AnEntity.class
					).setCacheable( true ).uniqueResult();
					assertNull( anEntity.otherEntity );
				}
		);

		assertEquals( 1, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );
	}

	@Test
	public void testQueryEntityAndNullManyToOne() {

		sessionFactory().getCache().evictAllRegions();
		sessionFactory().getStatistics().clear();

		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		sessionFactory().getStatistics().clear();

		final String queryString = "select e, e.otherEntity from AnEntity e left join e.otherEntity where e.id = " + id;

		doInHibernate(
				this::sessionFactory,
				session -> {
					final Object[] result = session.createQuery(
							queryString,
							Object[].class
					).setCacheable( true ).uniqueResult();
					assertEquals( 2, result.length );
					assertTrue( AnEntity.class.isInstance( result[0] ) );
					assertNull( result[1] );
				}
		);

		assertEquals( 0, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 1, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );

		sessionFactory().getStatistics().clear();

		doInHibernate(
				this::sessionFactory,
				session -> {
					final Object[] result = session.createQuery(
							queryString,
							Object[].class
					).setCacheable( true ).uniqueResult();
					assertEquals( 2, result.length );
					assertTrue( AnEntity.class.isInstance( result[0] ) );
					assertNull( result[1] );
				}
		);

		assertEquals( 1, getQueryStatistics( queryString ).getCacheHitCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCacheMissCount() );
		assertEquals( 0, getQueryStatistics( queryString ).getCachePutCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getPutCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getPutCount() );

		assertEquals( 1, getEntity2LCStatistics( AnEntity.class ).getHitCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getHitCount() );

		assertEquals( 0, getEntity2LCStatistics( AnEntity.class ).getMissCount() );
		assertEquals( 0, getEntity2LCStatistics( OtherEntity.class ).getMissCount() );
	}


	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	private SecondLevelCacheStatistics getEntity2LCStatistics(Class<?> className) {
		return sessionFactory().getStatistics()
				.getSecondLevelCacheStatistics( className.getName() );
	}

	private QueryStatistics getQueryStatistics(String queryString) {
		return sessionFactory().getStatistics().getQueryStatistics( queryString );
	}
	@Entity(name = "AnEntity")
	@Cacheable
	public static class AnEntity {
		@Id
		private int id;

		@ManyToOne
		private OtherEntity otherEntity;
	}

	@Entity(name = "OtherEntity")
	@Cacheable
	public static class OtherEntity implements Serializable {
		@Id
		private String firstName;

		@Id
		private String lastName;

		private String description;


		@Override
		public String toString() {
			return "OtherEntity{" +
					"firstName='" + firstName + '\'' +
					", lastName='" + lastName + '\'' +
					", description='" + description + '\'' +
					'}';
		}
	}
}
