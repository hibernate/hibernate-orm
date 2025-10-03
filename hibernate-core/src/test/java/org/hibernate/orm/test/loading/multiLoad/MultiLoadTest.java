/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.IncludeRemovals;
import org.hibernate.OrderedReturn;
import org.hibernate.SessionChecking;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.MultiKeyLoadHelper;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop" )
		}
)
@DomainModel(
		annotatedClasses = MultiLoadTest.SimpleEntity.class,
		sharedCacheMode = SharedCacheMode.ENABLE_SELECTIVE,
		accessType = AccessType.READ_WRITE
)
@SessionFactory( useCollectingStatementInspector = true )
public class MultiLoadTest {

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					for ( int i = 1; i <= 60; i++ ) {
						session.persist( new SimpleEntity( i, "Entity #" + i ) );
					}
				}
		);
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	public void testBasicMultiLoad(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();

					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids( 5 ) );
					assertEquals( 5, list.size() );

					final int paramCount = StringHelper.countUnquoted(
							statementInspector.getSqlQueries().get( 0 ),
							'?'
					);

					final Dialect dialect = session.getSessionFactory()
							.getJdbcServices()
							.getDialect();
					if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
						assertThat( paramCount, is( 1 ) );
					}
					else {
						assertThat( paramCount, is( 5 ) );
					}
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10984" )
	public void testUnflushedDeleteAndThenMultiLoadPart0(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleEntity s4 = session.getReference(SimpleEntity.class, 5);
					session.remove( s4 );

					assertFalse( Hibernate.isInitialized( s4 ) );

					// as a baseline, assert based on how load() handles it
					SimpleEntity s5 = session.getReference( SimpleEntity.class, 5 );
					assertNotNull( s5 );
					assertFalse( Hibernate.isInitialized( s5 ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10984" )
	public void testUnflushedDeleteAndThenMultiLoadPart1(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleEntity s4 = session.getReference( SimpleEntity.class, 5 );
					Hibernate.initialize( s4 );
					session.remove( s4 );

					// as a baseline, assert based on how load() handles it
					SimpleEntity s5 = session.getReference( SimpleEntity.class, 5 );
					assertNotNull( s5 );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10984" )
	public void testUnflushedDeleteAndThenMultiLoadPart2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleEntity s4 = session.getReference( SimpleEntity.class, 5 );
					Hibernate.initialize( s4 );
					session.remove( s4 );

					// and then, assert how get() handles it
					SimpleEntity s5 = session.get( SimpleEntity.class, 5 );
					assertNull( s5 );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10984" )
	public void testUnflushedDeleteAndThenMultiLoadPart3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleEntity s4 = session.getReference( SimpleEntity.class, 5 );
					Hibernate.initialize( s4 );
					session.remove( s4 );

					// finally assert how multiLoad handles it
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids( 56 ) );
					assertEquals( 56, list.size() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10984" )
	public void testUnflushedDeleteAndThenMultiLoadPart4(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					session.remove( session.getReference( SimpleEntity.class, 5 ) );

					// and then, assert how get() handles it
					SimpleEntity s5 = session.get( SimpleEntity.class, 5 );
					assertNull( s5 );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10617" )
	public void testDuplicatedRequestedIds(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered multiLoad
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( 1, 2, 3, 2, 2 );
					assertEquals( 5, list.size() );
					assertSame( list.get( 1 ), list.get( 3 ) );
					assertSame( list.get( 1 ), list.get( 4 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10617")
	public void testDuplicatedRequestedIdswithDisableOrderedReturn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// un-ordered multiLoad
					{
						final List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
								.enableOrderedReturn( false )
								.multiLoad( 1, 2, 3, 2, 2 );
						assertEquals( 3, list.size() );
					}

					{
						final List<SimpleEntity> list = session.findMultiple( SimpleEntity.class, List.of( 1, 2, 3, 2, 2 ), OrderedReturn.UNORDERED );
						assertEquals( 3, list.size() );

					}
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-10617" )
	public void testNonExistentIdRequest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered multiLoad
					{
						List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( 1, 699, 2 );
						assertEquals( 3, list.size() );
						assertNull( list.get( 1 ) );

						// un-ordered multiLoad
						list = session.byMultipleIds( SimpleEntity.class ).enableOrderedReturn( false ).multiLoad( 1, 699, 2 );
						assertEquals( 2, list.size() );
					}

					{
						final List<SimpleEntity> list = session.findMultiple( SimpleEntity.class, List.of(1, 699, 2), OrderedReturn.UNORDERED );
						assertEquals( 2, list.size() );
					}
				}
		);
	}

	@Test
	public void testBasicMultiLoadWithManagedAndNoChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity first = session.byId( SimpleEntity.class ).load( 1 );
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids( 56 ) );
					assertEquals( 56, list.size() );
					// this check is HIGHLY specific to implementation in the batch loader
					// which puts existing managed entities first...
					assertSame( first, list.get( 0 ) );
				}
		);
	}

	@Test
	public void testBasicMultiLoadWithManagedAndChecking(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity first;
					{
						final List<SimpleEntity> list;
						first = session.byId( SimpleEntity.class ).load( 1 );
						list = session.byMultipleIds( SimpleEntity.class )
								.enableSessionCheck( true )
								.multiLoad( ids( 56 ) );
						assertEquals( 56, list.size() );
						// this check is HIGHLY specific to implementation in the batch loader
						// which puts existing managed entities first...
						assertSame( first, list.get( 0 ) );
					}

					{
						final List<SimpleEntity> list = session.findMultiple( SimpleEntity.class, idList(56), SessionChecking.ENABLED );
						assertEquals( 56, list.size() );
						// this check is HIGHLY specific to implementation in the batch loader
						// which puts existing managed entities first...
						assertSame( first, list.get( 0 ) );
					}
				}
		);
	}

	@Test
	public void testBasicMultiLoadWithManagedAndNoCheckingProxied(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity first = session.byId( SimpleEntity.class ).getReference( 1 );
					List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class ).multiLoad( ids( 56 ) );
					assertEquals( 56, list.size() );
					// this check is HIGHLY specific to implementation in the batch loader
					// which puts existing managed entities first...
					assertSame( first, list.get( 0 ) );
				}
		);
	}

	@Test
	public void testBasicMultiLoadWithManagedAndCheckingProxied(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity first = session.byId( SimpleEntity.class ).getReference( 1 );
					{
						final List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
								.enableSessionCheck( true )
								.multiLoad( ids( 56 ) );
						assertEquals( 56, list.size() );
						// this check is HIGHLY specific to implementation in the batch loader
						// which puts existing managed entities first...
						assertSame( first, list.get( 0 ) );
					}

					session.evict( first );
					first = session.byId( SimpleEntity.class ).getReference( 1 );

					{
						final List<SimpleEntity> list = session.findMultiple( SimpleEntity.class, idList(56), SessionChecking.ENABLED );
						assertEquals( 56, list.size() );
						// this check is HIGHLY specific to implementation in the batch loader
						// which puts existing managed entities first...
						assertSame( first, list.get( 0 ) );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12944")
	public void testMultiLoadFrom2ndLevelCache(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.getSessionFactory().getCache().evictAll();

		final Statistics statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
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

		scope.inTransaction(
				session -> {
					// Validate that the entity is still in the Level 2 cache
					assertTrue( session.getSessionFactory().getCache().containsEntity( SimpleEntity.class, 2 ) );

					statementInspector.clear();
					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.NORMAL )
								.enableSessionCheck( true )
								.multiLoad( ids( 3 ) );
						assertEquals( 3, entities.size() );
						assertEquals( 1, statistics.getSecondLevelCacheHitCount() );

						for ( SimpleEntity entity : entities ) {
							assertTrue( session.contains( entity ) );
						}

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.findMultiple( SimpleEntity.class, idList( 3 ),
								CacheMode.NORMAL,
								SessionChecking.ENABLED
						);
						assertEquals( 3, entities.size() );
						assertEquals( 1, statistics.getSecondLevelCacheHitCount() );

						for ( SimpleEntity entity : entities ) {
							assertTrue( session.contains( entity ) );
						}

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12944")
	public void testUnorderedMultiLoadFrom2ndLevelCache(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.getSessionFactory().getCache().evictAll();

		final Statistics statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
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

		scope.inTransaction(
				session -> {

					// Validate that the entity is still in the Level 2 cache
					assertTrue( session.getSessionFactory().getCache().containsEntity( SimpleEntity.class, 2 ) );

					statementInspector.clear();

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.NORMAL )
								.enableSessionCheck( true )
								.enableOrderedReturn( false )
								.multiLoad( ids( 3 ) );
						assertEquals( 3, entities.size() );
						assertEquals( 1, statistics.getSecondLevelCacheHitCount() );

						for ( SimpleEntity entity : entities ) {
							assertTrue( session.contains( entity ) );
						}
						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.findMultiple( SimpleEntity.class, idList( 3 ),
								CacheMode.NORMAL,
								SessionChecking.ENABLED,
								OrderedReturn.UNORDERED
						);
						assertEquals( 3, entities.size() );
						assertEquals( 1, statistics.getSecondLevelCacheHitCount() );

						for ( SimpleEntity entity : entities ) {
							assertTrue( session.contains( entity ) );
						}
						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12944")
	public void testOrderedMultiLoadFrom2ndLevelCachePendingDelete(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					statementInspector.clear();

					{
						// Multi-load 3 items and ensure that it pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.NORMAL )
								.enableSessionCheck( true )
								.enableOrderedReturn( true )
								.multiLoad( ids( 3 ) );
						assertEquals( 3, entities.size() );

						assertNull( entities.get( 1 ) );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}

					{
						// Multi-load 3 items and ensure that it pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.findMultiple( SimpleEntity.class, idList( 3 ),
								CacheMode.NORMAL,
								SessionChecking.ENABLED,
								OrderedReturn.ORDERED
						);
						assertEquals( 3, entities.size() );

						assertNull( entities.get( 1 ) );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12944")
	public void testOrderedMultiLoadFrom2ndLevelCachePendingDeleteReturnRemoved(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					statementInspector.clear();

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.NORMAL )
								.enableSessionCheck( true )
								.enableOrderedReturn( true )
								.enableReturnOfDeletedEntities( true )
								.multiLoad( ids( 3 ) );
						assertEquals( 3, entities.size() );

						SimpleEntity deletedEntity = entities.get( 1 );
						assertNotNull( deletedEntity );

						EntityEntry entry = session.getPersistenceContext()
								.getEntry( deletedEntity );
						assertTrue( entry.getStatus().isDeletedOrGone() );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.findMultiple( SimpleEntity.class, idList( 3 ),
								CacheMode.NORMAL,
								SessionChecking.ENABLED,
								OrderedReturn.ORDERED,
								IncludeRemovals.INCLUDE
						);
						assertEquals( 3, entities.size() );

						SimpleEntity deletedEntity = entities.get( 1 );
						assertNotNull( deletedEntity );

						EntityEntry entry = session.getPersistenceContext()
								.getEntry( deletedEntity );
						assertTrue( entry.getStatus().isDeletedOrGone() );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}
				} );
	}

	@Test
	@JiraKey(value = "HHH-12944")
	public void testUnorderedMultiLoadFrom2ndLevelCachePendingDelete(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					statementInspector.clear();

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.NORMAL )
								.enableSessionCheck( true )
								.enableOrderedReturn( false )
								.multiLoad( ids( 3 ) );
						assertEquals( 3, entities.size() );

						assertTrue( entities.stream().anyMatch( Objects::isNull ) );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.findMultiple( SimpleEntity.class, idList( 3 ),
								CacheMode.NORMAL,
								SessionChecking.ENABLED,
								OrderedReturn.UNORDERED
						);
						assertEquals( 3, entities.size() );

						assertTrue( entities.stream().anyMatch( Objects::isNull ) );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}
				} );
	}

	@Test
	@JiraKey(value = "HHH-12944")
	public void testUnorderedMultiLoadFrom2ndLevelCachePendingDeleteReturnRemoved(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					session.remove( session.find( SimpleEntity.class, 2 ) );

					statementInspector.clear();

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities = session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.NORMAL )
								.enableSessionCheck( true )
								.enableOrderedReturn( false )
								.enableReturnOfDeletedEntities( true )
								.multiLoad( ids( 3 ) );
						assertEquals( 3, entities.size() );

						SimpleEntity deletedEntity = entities.stream().filter( simpleEntity -> simpleEntity.getId()
								.equals( 2 ) ).findAny().orElse( null );
						assertNotNull( deletedEntity );

						EntityEntry entry = session.getPersistenceContext().getEntry( deletedEntity );
						assertTrue( entry.getStatus().isDeletedOrGone() );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}

					{
						// Multiload 3 items and ensure that multiload pulls 2 from the database & 1 from the cache.
						final List<SimpleEntity> entities =session.findMultiple( SimpleEntity.class, idList( 3 ),
							CacheMode.NORMAL,
							SessionChecking.ENABLED,
							OrderedReturn.UNORDERED,
							IncludeRemovals.INCLUDE
					);
						assertEquals( 3, entities.size() );

						SimpleEntity deletedEntity = entities.stream().filter( simpleEntity -> simpleEntity.getId()
								.equals( 2 ) ).findAny().orElse( null );
						assertNotNull( deletedEntity );

						EntityEntry entry = session.getPersistenceContext().getEntry( deletedEntity );
						assertTrue( entry.getStatus().isDeletedOrGone() );

						final int paramCount = StringHelper.countUnquoted(
								statementInspector.getSqlQueries().get( 0 ),
								'?'
						);

						final Dialect dialect = session.getSessionFactory().getJdbcServices().getDialect();
						if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
							assertThat( paramCount, is( 1 ) );
						}
						else {
							assertThat( paramCount, is( 2 ) );
						}
					}
				} );
	}

	@Test
	public void testMultiLoadWithCacheModeIgnore(SessionFactoryScope scope) {
		// do the multi-load, telling Hibernate to IGNORE the L2 cache -
		//		the end result should be that the cache is (still) empty afterwards
		List<SimpleEntity> list = scope.fromTransaction(
				session ->
						session.byMultipleIds( SimpleEntity.class )
								.with( CacheMode.IGNORE )
								.multiLoad( ids( 56 ) )
		);
		assertEquals( 56, list.size() );
		for ( SimpleEntity entity : list ) {
			assertFalse( scope.getSessionFactory().getCache().containsEntity( SimpleEntity.class, entity.getId() ) );
		}
	}

	@Test
	public void testMultiLoadClearsBatchFetchQueue(SessionFactoryScope scope) {
		final EntityKey entityKey = new EntityKey(
				1,
				scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( SimpleEntity.class.getName() )
		);

		scope.inTransaction(
				session -> {
					// create a proxy, which should add an entry to the BatchFetchQueue
					SimpleEntity first = session.byId( SimpleEntity.class ).getReference( 1 );
					assertTrue( session.getPersistenceContext()
										.getBatchFetchQueue()
										.containsEntityKey( entityKey ) );

					// now bulk load, which should clean up the BatchFetchQueue entry
					{
						final List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
								.enableSessionCheck( true )
								.multiLoad( ids( 56 ) );
						assertEquals( 56, list.size() );
						assertFalse( session.getPersistenceContext()
								.getBatchFetchQueue()
								.containsEntityKey( entityKey ) );
					}

					{
						final List<SimpleEntity> list = session.findMultiple( SimpleEntity.class, idList( 56 ),
								SessionChecking.ENABLED );
						assertEquals( 56, list.size() );
						assertFalse( session.getPersistenceContext()
								.getBatchFetchQueue()
								.containsEntityKey( entityKey ) );
					}
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

	private List<Integer> idList(int count) {
		List<Integer> ids = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			ids.add(i);
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
