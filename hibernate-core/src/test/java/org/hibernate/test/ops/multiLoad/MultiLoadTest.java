/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops.multiLoad;

import java.util.List;
import java.util.Objects;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;

import org.hibernate.CacheMode;
import org.hibernate.annotations.BatchSize;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class MultiLoadTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString() );
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );

		metadataBuilder.applySharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE );
		metadataBuilder.applyAccessType( AccessType.READ_WRITE );
	}

	@Before
	public void before() {
		inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					for ( int i = 1; i <= 60; i++ ) {
						session.save( new SimpleEntity( i, "Entity #" + i ) );
					}
				}
		);
	}

	@After
	public void after() {
		inTransaction(
				session -> {
					session.createQuery( "delete SimpleEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testBasicMultiLoad() {
		inTransaction(
				session -> {
					sqlStatementInterceptor.getSqlQueries().clear();

					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids( 5 ) );
					assertEquals( 5, list.size() );

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 5 ) );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10984" )
	public void testUnflushedDeleteAndThenMultiLoad() {
		inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					session.delete( session.load( SimpleEntity.class, 5 ) );

					// as a baseline, assert based on how load() handles it
					SimpleEntity s5 = session.load( SimpleEntity.class, 5 );
					assertNotNull( s5 );

					// and then, assert how get() handles it
					s5 = session.get( SimpleEntity.class, 5 );
					assertNull( s5 );

					// finally assert how multiLoad handles it
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids(56) );
					assertEquals( 56, list.size() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10617" )
	public void testDuplicatedRequestedIds() {
		inTransaction(
				session -> {
					// ordered multiLoad
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( 1, 2, 3, 2, 2 );
					assertEquals( 5, list.size() );
					assertSame( list.get( 1 ), list.get( 3 ) );
					assertSame( list.get( 1 ), list.get( 4 ) );

					// un-ordered multiLoad
					list = session.byMultipleIds( SimpleEntity.class ).enableOrderedReturn( false ).multiLoad( 1, 2, 3, 2, 2 );
					assertEquals( 3, list.size() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10617" )
	public void testNonExistentIdRequest() {
		inTransaction(
				session -> {
					// ordered multiLoad
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( 1, 699, 2 );
					assertEquals( 3, list.size() );
					assertNull( list.get( 1 ) );

					// un-ordered multiLoad
					list = session.byMultipleIds( SimpleEntity.class ).enableOrderedReturn( false ).multiLoad( 1, 699, 2 );
					assertEquals( 2, list.size() );
				}
		);
	}

	@Test
	public void testBasicMultiLoadWithManagedAndNoChecking() {
		inTransaction(
				session -> {
					SimpleEntity first = session.byId( SimpleEntity.class ).load( 1 );
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids(56) );
					assertEquals( 56, list.size() );
					// this check is HIGHLY specific to implementation in the batch loader
					// which puts existing managed entities first...
					assertSame( first, list.get( 0 ) );
				}
		);
	}

	@Test
	public void testBasicMultiLoadWithManagedAndChecking() {
		inTransaction(
				session -> {
					SimpleEntity first = session.byId( SimpleEntity.class ).load( 1 );
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).enableSessionCheck( true ).multiLoad( ids(56) );
					assertEquals( 56, list.size() );
					// this check is HIGHLY specific to implementation in the batch loader
					// which puts existing managed entities first...
					assertSame( first, list.get( 0 ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12944")
	public void testMultiLoadFrom2ndLevelCache() {
		sessionFactory().getCache().evictAll();

		final Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		inTransaction(
				session -> {
					// Load 1 of the items directly
					SimpleEntity entity = session.get( SimpleEntity.class, 2 );
					assertNotNull( entity );

					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );
					assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 1, statistics.getSecondLevelCachePutCount() );
					assertTrue( session.getSessionFactory().getCache().containsEntity( SimpleEntity.class, 2 ) );
				}
		);

		statistics.clear();

		inTransaction(
				session -> {
					// Validate that the entity is still in the Level 2 cache
					assertTrue( session.getSessionFactory().getCache().containsEntity( SimpleEntity.class, 2 ) );

					sqlStatementInterceptor.getSqlQueries().clear();

					// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
					List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.NORMAL )
							.enableSessionCheck( true )
							.multiLoad( ids( 3 ) );

					assertEquals( 3, entities.size() );
					assertEquals( 1, statistics.getSecondLevelCacheHitCount() );

					for ( SimpleEntity entity: entities ) {
						assertTrue( session.contains( entity ) );
					}

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 2 ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12944")
	public void testUnorderedMultiLoadFrom2ndLevelCache() {
		sessionFactory().getCache().evictAll();

		final Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		inTransaction(
				session -> {
					// Load 1 of the items directly
					final SimpleEntity entity = session.get( SimpleEntity.class, 2 );
					assertNotNull( entity );

					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );
					assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 1, statistics.getSecondLevelCachePutCount() );
					assertTrue( session.getSessionFactory().getCache().containsEntity( SimpleEntity.class, 2 ) );
				}
		);

		statistics.clear();

		inTransaction(
				session -> {
					// Validate that the entity is still in the Level 2 cache
					assertTrue( session.getSessionFactory().getCache().containsEntity( SimpleEntity.class, 2 ) );

					sqlStatementInterceptor.getSqlQueries().clear();

					// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
					List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.NORMAL )
							.enableSessionCheck( true )
							.enableOrderedReturn( false )
							.multiLoad( ids( 3 ) );
					assertEquals( 3, entities.size() );
					assertEquals( 1, statistics.getSecondLevelCacheHitCount() );

					for(SimpleEntity entity: entities) {
						assertTrue( session.contains( entity ) );
					}

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 2 ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12944")
	public void testOrderedMultiLoadFrom2ndLevelCachePendingDelete() {
		inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					sqlStatementInterceptor.getSqlQueries().clear();

					// Multi-load 3 items and ensure that it pulls 2 from the database & 1 from the cache.
					List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.NORMAL )
							.enableSessionCheck( true )
							.enableOrderedReturn( true )
							.multiLoad( ids( 3 ) );
					assertEquals( 3, entities.size() );

					assertNull( entities.get(1) );

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 2 ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12944")
	public void testOrderedMultiLoadFrom2ndLevelCachePendingDeleteReturnRemoved() {
		inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					sqlStatementInterceptor.getSqlQueries().clear();

					// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
					List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.NORMAL )
							.enableSessionCheck( true )
							.enableOrderedReturn( true )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids( 3 ) );
					assertEquals( 3, entities.size() );

					SimpleEntity deletedEntity = entities.get(1);
					assertNotNull( deletedEntity );

					final EntityEntry entry = ((SharedSessionContractImplementor) session).getPersistenceContext().getEntry( deletedEntity );
					assertTrue( entry.getStatus() == Status.DELETED || entry.getStatus() == Status.GONE );

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 2 ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12944")
	public void testUnorderedMultiLoadFrom2ndLevelCachePendingDelete() {
		inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					sqlStatementInterceptor.getSqlQueries().clear();

					// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
					List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.NORMAL )
							.enableSessionCheck( true )
							.enableOrderedReturn( false )
							.multiLoad( ids( 3 ) );
					assertEquals( 3, entities.size() );

					assertTrue( entities.stream().anyMatch( Objects::isNull ) );

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 2 ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12944")
	public void testUnorderedMultiLoadFrom2ndLevelCachePendingDeleteReturnRemoved() {
		inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					sqlStatementInterceptor.getSqlQueries().clear();

					// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
					List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.NORMAL )
							.enableSessionCheck( true )
							.enableOrderedReturn( false )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids( 3 ) );
					assertEquals( 3, entities.size() );

					SimpleEntity deletedEntity = entities.stream().filter( simpleEntity -> simpleEntity.getId().equals( 2 ) ).findAny().orElse( null );
					assertNotNull( deletedEntity );

					final EntityEntry entry = ((SharedSessionContractImplementor) session).getPersistenceContext().getEntry( deletedEntity );
					assertTrue( entry.getStatus() == Status.DELETED || entry.getStatus() == Status.GONE );

					final int paramCount = StringHelper.countUnquoted( sqlStatementInterceptor.getSqlQueries().getFirst(), '?' );
					assertThat( paramCount, is( 2 ) );
				}
		);
	}

	@Test
	public void testMultiLoadWithCacheModeIgnore() {
		// do the multi-load, telling Hibernate to IGNORE the L2 cache -
		//		the end result should be that the cache is (still) empty afterwards
		inTransaction(
				session -> {
					final List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
							.with( CacheMode.IGNORE )
							.multiLoad( ids(56) );

					assertEquals( 56, list.size() );
					for ( SimpleEntity entity : list ) {
						assertFalse( sessionFactory().getCache().containsEntity( SimpleEntity.class, entity.getId() ) );
					}
				}
		);
	}

	@Test
	public void testMultiLoadClearsBatchFetchQueue() {
		final EntityKey entityKey = new EntityKey(
				1,
				sessionFactory().getMetamodel().entityPersister( SimpleEntity.class.getName() )
		);

		inTransaction(
				session -> {
					// create a proxy, which should add an entry to the BatchFetchQueue
					SimpleEntity first = session.byId( SimpleEntity.class ).getReference( 1 );
					assertTrue( ( (SessionImplementor) session ).getPersistenceContext().getBatchFetchQueue().containsEntityKey( entityKey ) );

					// now bulk load, which should clean up the BatchFetchQueue entry
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).enableSessionCheck( true ).multiLoad( ids(56) );

					assertEquals( 56, list.size() );
					assertFalse( ( (SessionImplementor) session ).getPersistenceContext().getBatchFetchQueue().containsEntityKey( entityKey ) );
				}
		);
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
