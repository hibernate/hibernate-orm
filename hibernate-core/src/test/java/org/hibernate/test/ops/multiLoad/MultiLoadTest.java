/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops.multiLoad;

import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class MultiLoadTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );

		metadataBuilder.applySharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE );
		metadataBuilder.applyAccessType( AccessType.READ_WRITE );
	}

	@Before
	public void before() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();
		session.setCacheMode( CacheMode.IGNORE );
		for ( int i = 1; i <= 60; i++ ) {
			session.save( new SimpleEntity( i, "Entity #" + i ) );
		}
		session.getTransaction().commit();
		session.close();
	}

	@After
	public void after() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();
		session.createQuery( "delete SimpleEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicMultiLoad() {
		Session session = openSession();
		session.getTransaction().begin();
		List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids(56) );
		assertEquals( 56, list.size() );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicMultiLoadWithManagedAndNoChecking() {
		Session session = openSession();
		session.getTransaction().begin();
		SimpleEntity first = session.byId( SimpleEntity.class ).load( 1 );
		List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids(56) );
		assertEquals( 56, list.size() );
		// this check is HIGHLY specific to implementation in the batch loader
		// which puts existing managed entities first...
		assertSame( first, list.get( 0 ) );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicMultiLoadWithManagedAndChecking() {
		Session session = openSession();
		session.getTransaction().begin();
		SimpleEntity first = session.byId( SimpleEntity.class ).load( 1 );
		List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).enableSessionCheck( true ).multiLoad( ids(56) );
		assertEquals( 56, list.size() );
		// this check is HIGHLY specific to implementation in the batch loader
		// which puts existing managed entities first...
		assertSame( first, list.get( 0 ) );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testMultiLoadWithCacheModeIgnore() {
		// do the multi-load, telling Hibernate to IGNORE the L2 cache -
		//		the end result should be that the cache is (still) empty afterwards
		Session session = openSession();
		session.getTransaction().begin();
		List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
				.with( CacheMode.IGNORE )
				.multiLoad( ids(56) );
		session.getTransaction().commit();
		session.close();

		assertEquals( 56, list.size() );
		for ( SimpleEntity entity : list ) {
			assertFalse( sessionFactory().getCache().containsEntity( SimpleEntity.class, entity.getId() ) );
		}
	}

	@Test
	public void testMultiLoadClearsBatchFetchQueue() {
		final EntityKey entityKey = new EntityKey(
				1,
				sessionFactory().getEntityPersister( SimpleEntity.class.getName() )
		);

		Session session = openSession();
		session.getTransaction().begin();
		// create a proxy, which should add an entry to the BatchFetchQueue
		SimpleEntity first = session.byId( SimpleEntity.class ).getReference( 1 );
		assertTrue( ( (SessionImplementor) session ).getPersistenceContext().getBatchFetchQueue().containsEntityKey( entityKey ) );

		// now bulk load, which should clean up the BatchFetchQueue entry
		List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).enableSessionCheck( true ).multiLoad( ids(56) );

		assertEquals( 56, list.size() );
		assertFalse( ( (SessionImplementor) session ).getPersistenceContext().getBatchFetchQueue().containsEntityKey( entityKey ) );

		session.getTransaction().commit();
		session.close();
	}

	private Integer[] ids(int count) {
		Integer[] ids = new Integer[count];
		for ( int i = 1; i <= count; i++ ) {
			ids[i-1] = i;
		}
		return ids;
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "SimpleEntity" )
	@Cacheable()
	@BatchSize( size = 15 )
	public static class SimpleEntity {
		Integer id;
		String text;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
