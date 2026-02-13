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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = NativeQueryCacheWithExtraColumnsTest.TestUser.class)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
})
@Jira("https://hibernate.atlassian.net/browse/HHH-20176")
@RequiresDialect(H2Dialect.class)
public class NativeQueryCacheWithExtraColumnsTest {

	private static final String NATIVE_QUERY = "SELECT u1.* FROM TEST_USER u1, TEST_USER u2 WHERE u2.ID = u1.ID";

	private static final String NATIVE_QUERY_EXTRA_COLS_FIRST =
			"SELECT u1.EXTRA_COL1, u1.EXTRA_COL2, u1.ID, u1.NAME, u1.EMAIL, u1.AGE, u1.ADDRESS, u1.PHONE"
			+ " FROM TEST_USER u1";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Add extra columns to the table that are not mapped in the entity
			session.createNativeMutationQuery( "ALTER TABLE TEST_USER ADD COLUMN IF NOT EXISTS EXTRA_COL1 VARCHAR(50)" )
					.executeUpdate();
			session.createNativeMutationQuery( "ALTER TABLE TEST_USER ADD COLUMN IF NOT EXISTS EXTRA_COL2 VARCHAR(50)" )
					.executeUpdate();

			// Insert test data with extra columns
			session.createNativeMutationQuery(
							"INSERT INTO TEST_USER (ID, NAME, EMAIL, AGE, ADDRESS, PHONE, EXTRA_COL1, EXTRA_COL2) VALUES "
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
	public void testNativeQueryWithCacheAndExtraColumns(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query - should populate the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query - should use the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	private static void assertTestUser(List<TestUser> users) {
		assertThat( users ).hasSize( 1 );
		final var user = users.get( 0 );
		assertThat( user.getName() ).isEqualTo( "john" );
		assertThat( user.getEmail() ).isEqualTo( "john@test.com" );
		assertThat( user.getAge() ).isEqualTo( 30 );
		assertThat( user.getAddress() ).isEqualTo( "ny" );
		assertThat( user.getPhone() ).isEqualTo( "123456" );
	}

	@Test
	public void testNativeQueryWithCacheAndTupleResult(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query - should populate the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY, Tuple.class );
			query.setCacheable( true );
			assertTestUserTuple( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query - should use the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY, Tuple.class );
			query.setCacheable( true );
			assertTestUserTuple( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	private static void assertTestUserTuple(List<Tuple> tuples) {
		assertThat( tuples ).hasSize( 1 );
		final var tuple = tuples.get( 0 );
		assertThat( tuple.get( "NAME", String.class ) ).isEqualTo( "john" );
		assertThat( tuple.get( "EMAIL", String.class ) ).isEqualTo( "john@test.com" );
		assertThat( tuple.get( "AGE", Integer.class ) ).isEqualTo( 30 );
		assertThat( tuple.get( "ADDRESS", String.class ) ).isEqualTo( "ny" );
		assertThat( tuple.get( "PHONE", String.class ) ).isEqualTo( "123456" );
		assertThat( tuple.get( "EXTRA_COL1", String.class ) ).isEqualTo( "ext1" );
		assertThat( tuple.get( "EXTRA_COL2", String.class ) ).isEqualTo( "ext2" );
	}

	@Test
	public void testExtraColumnsBefore(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query - should populate the cache (works fine, reads from live ResultSet)
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query - should use the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, TestUser.class );
			query.setCacheable( true );
			assertTestUser( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	@Test
	public void testExtraColumnsBeforeTuple(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// First query - should populate the cache (works fine, reads from live ResultSet)
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, Tuple.class );
			query.setCacheable( true );
			assertTestUserTuple( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		// Second query - should use the cache
		scope.inSession( session -> {
			final var query = session.createNativeQuery( NATIVE_QUERY_EXTRA_COLS_FIRST, Tuple.class );
			query.setCacheable( true );
			assertTestUserTuple( query.getResultList() );
		} );

		assertQueryCacheStatistics( statistics, 1, 0, 0 );
	}

	private static void assertQueryCacheStatistics(Statistics statistics, int hits, int misses, int puts) {
		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( hits );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( misses );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( puts );
		statistics.clear();
	}

	@Entity(name = "TestUser")
	@Table(name = "TEST_USER")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class TestUser {
		@Id
		@Column(name = "ID")
		private Long id;

		@Column(name = "NAME")
		private String name;

		@Column(name = "EMAIL")
		private String email;

		@Column(name = "AGE")
		private Integer age;

		@Column(name = "ADDRESS")
		private String address;

		@Column(name = "PHONE")
		private String phone;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}
	}
}
