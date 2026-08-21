/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		NativeQueryCacheMixedReturnTypeTest.TestUser.class,
		NativeQueryCacheMixedReturnTypeTest.TestUserProfile.class,
		NativeQueryCacheMixedReturnTypeTest.TestUserLongAge.class
})
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
})
@RequiresDialect(H2Dialect.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-20231")
public class NativeQueryCacheMixedReturnTypeTest {

	private static final String NATIVE_QUERY_EXTRA_COLS_LAST =
			"select u1.id, u1.name, u1.email, u1.age, u1.address, u1.phone, u1.extra_col1, u1.extra_col2 from test_user u1";
	private static final String NATIVE_QUERY_EXTRA_COLS_FIRST =
			"select u1.extra_col1, u1.extra_col2, u1.id, u1.name, u1.email, u1.age, u1.address, u1.phone from test_user u1";
	private static final String NATIVE_QUERY_EXTRA_COLS_SCATTERED =
			"select u1.id, u1.extra_col1, u1.name, u1.email, u1.extra_col2, u1.age, u1.address, u1.phone from test_user u1";
	private static final String NATIVE_QUERY_ENTITY_COLS =
			"select u1.id, u1.name, u1.email, u1.age, u1.address, u1.phone from test_user u1";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Add extra columns to the table that are not mapped in the entity
			session.createNativeMutationQuery( "alter table test_user add column if not exists extra_col1 varchar(50)" )
					.executeUpdate();
			session.createNativeMutationQuery( "alter table test_user add column if not exists extra_col2 varchar(50)" )
					.executeUpdate();

			// Insert test data with extra columns
			session.createNativeMutationQuery(
							"insert into test_user (id, name, email, age, address, phone, extra_col1, extra_col2) values "
							+ "(1, 'john', 'john@test.com', 30, 'ny', '123456', 'ext1', 'ext2')" )
					.executeUpdate();
		} );
	}

	@AfterEach
	public void cleanCache(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testEntityThenTuple(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with entity return type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_LAST, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with Tuple return type - cached data is incompatible (entity cached
		// fewer columns), so re-executes and re-populates the cache with complete data
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_LAST, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Third query with Tuple return type - reads from the re-populated cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_LAST, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testTupleThenEntity(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with Tuple return type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_LAST, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with entity return type - reads from the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_LAST, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testEntityThenTupleSameColumnCount(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with entity return type - populates the cache.
		// The query selects exactly the entity-mapped columns, so cached row size
		// matches the column count (no extra columns).
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_ENTITY_COLS, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with Tuple return type - same column count as entity, cached data
		// should be compatible
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_ENTITY_COLS, Tuple.class );
			query.setCacheable( true );
			assertTestUserTuple( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testEntityThenTupleExtraColsFirst(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with entity return type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with Tuple return type - cached data is incompatible (entity cached
		// fewer columns), so re-executes and re-populates the cache with complete data
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Third query with Tuple return type - reads from the re-populated cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testTupleThenEntityExtraColsFirst(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with Tuple return type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with entity return type - reads from the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testEntityThenTupleExtraColsScattered(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with entity return type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with Tuple return type - cached data is incompatible (entity cached
		// fewer columns), so re-executes and re-populates the cache with complete data
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Third query with Tuple return type - reads from the re-populated cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testTupleThenEntityExtraColsScattered(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with Tuple return type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, Tuple.class );
			query.setCacheable( true );
			final var tuple = query.getSingleResult();
			assertTestUserTuple( tuple );
			assertThat( tuple.get( "extra_col1", String.class ) ).isEqualTo( "ext1" );
			assertThat( tuple.get( "extra_col2", String.class ) ).isEqualTo( "ext2" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with entity return type - reads from the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testUserProfileThenTestUser(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with TestUserProfile (maps extra_col1, not age) - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, TestUserProfile.class );
			query.setCacheable( true );
			final var profile = query.getSingleResult();
			assertThat( profile.name ).isEqualTo( "john" );
			assertThat( profile.email ).isEqualTo( "john@test.com" );
			assertThat( profile.extraCol1 ).isEqualTo( "ext1" );
			assertThat( profile.address ).isEqualTo( "ny" );
			assertThat( profile.phone ).isEqualTo( "123456" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with TestUser (maps age, not extra_col1) - stored mapping is
		// incompatible (no entry for age position), so re-executes and re-populates
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );
	}

	@Test
	public void testTestUserThenUserProfile(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with TestUser (maps age, not extra_col1) - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with TestUserProfile (maps extra_col1, not age) - stored mapping is
		// incompatible (no entry for extra_col1 position), so re-executes and re-populates
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_SCATTERED, TestUserProfile.class );
			query.setCacheable( true );
			final var profile = query.getSingleResult();
			assertThat( profile.name ).isEqualTo( "john" );
			assertThat( profile.email ).isEqualTo( "john@test.com" );
			assertThat( profile.extraCol1 ).isEqualTo( "ext1" );
			assertThat( profile.address ).isEqualTo( "ny" );
			assertThat( profile.phone ).isEqualTo( "123456" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );
	}

	private static void assertQueryCacheStatistics(Statistics statistics, int hits, int misses, int puts) {
		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( hits );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( misses );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( puts );
		statistics.clear();
	}

	private static void assertTestUser(TestUser user) {
		assertThat( user.name ).isEqualTo( "john" );
		assertThat( user.email ).isEqualTo( "john@test.com" );
		assertThat( user.age ).isEqualTo( 30 );
		assertThat( user.address ).isEqualTo( "ny" );
		assertThat( user.phone ).isEqualTo( "123456" );
	}

	private static void assertTestUserTuple(Tuple tuple) {
		assertThat( tuple.get( "name", String.class ) ).isEqualTo( "john" );
		assertThat( tuple.get( "email", String.class ) ).isEqualTo( "john@test.com" );
		assertThat( tuple.get( "age", Integer.class ) ).isEqualTo( 30 );
		assertThat( tuple.get( "address", String.class ) ).isEqualTo( "ny" );
		assertThat( tuple.get( "phone", String.class ) ).isEqualTo( "123456" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Tests for same SQL with compatible but different scalar Java types.
	// When the cached value's Java type differs from the reader's expected type,
	// the cache hit is treated as incompatible and the query is re-executed.
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testScalarIntegerThenLong(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final String nativeQuery = "select u1.age from test_user u1";

		// First query with Integer result type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( nativeQuery, Integer.class );
			query.setCacheable( true );
			assertThat( query.getSingleResult() ).isEqualTo( 30 );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with Long result type - same SQL, same position, different Java type.
		// Cached data has incompatible type, so re-executes and re-populates the cache.
		scope.inSession( session -> {
			final var query = session.createNativeQuery( nativeQuery, Long.class );
			query.setCacheable( true );
			assertThat( query.getSingleResult() ).isEqualTo( 30L );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Third query with Long result type - now reads from the re-populated cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( nativeQuery, Long.class );
			query.setCacheable( true );
			assertThat( query.getSingleResult() ).isEqualTo( 30L );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testScalarLongThenInteger(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final String nativeQuery = "select u1.age from test_user u1";

		// First query with Long result type - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( nativeQuery, Long.class );
			query.setCacheable( true );
			assertThat( query.getSingleResult() ).isEqualTo( 30L );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with Integer result type - cached data has incompatible type,
		// so re-executes and re-populates the cache.
		scope.inSession( session -> {
			final var query = session.createNativeQuery( nativeQuery, Integer.class );
			query.setCacheable( true );
			assertThat( query.getSingleResult() ).isEqualTo( 30 );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Third query with Integer result type - now reads from the re-populated cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( nativeQuery, Integer.class );
			query.setCacheable( true );
			assertThat( query.getSingleResult() ).isEqualTo( 30 );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Test for entities with the same columns but different Java types.
	// TestUser maps age as Integer, TestUserLongAge maps age as Long.
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testEntityIntegerAgeThenEntityLongAge(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query with TestUser (age as Integer) - populates the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_ENTITY_COLS, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getSingleResult() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query with TestUserLongAge (age as Long) - same SQL, same columns,
		// but age is mapped with a different Java type. Cached data has incompatible type,
		// so re-executes and re-populates the cache.
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_ENTITY_COLS, TestUserLongAge.class );
			query.setCacheable( true );
			final var user = query.getSingleResult();
			assertThat( user.name ).isEqualTo( "john" );
			assertThat( user.email ).isEqualTo( "john@test.com" );
			assertThat( user.age ).isEqualTo( 30L );
			assertThat( user.address ).isEqualTo( "ny" );
			assertThat( user.phone ).isEqualTo( "123456" );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Third query with TestUserLongAge - now reads from the re-populated cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_ENTITY_COLS, TestUserLongAge.class );
			query.setCacheable( true );
			final var user = query.getSingleResult();
			assertThat( user.age ).isEqualTo( 30L );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Entity(name = "TestUser")
	@Table(name = "test_user")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class TestUser {
		@Id
		Long id;
		String name;
		String email;
		Integer age;
		String address;
		String phone;
	}

	@Entity(name = "TestUserProfile")
	@Table(name = "test_user")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class TestUserProfile {
		@Id
		Long id;
		String name;
		String email;
		@Column(name = "extra_col1")
		String extraCol1;
		// Does NOT map age — different column subset than TestUser
		String address;
		String phone;
	}

	@Entity(name = "TestUserLongAge")
	@Table(name = "test_user")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class TestUserLongAge {
		@Id
		Long id;
		String name;
		String email;
		Long age;
		String address;
		String phone;
	}
}
