/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import java.time.Instant;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-17997")
@Jpa(
		annotatedClasses = {
				CachingWithTriggerTest.TestEntity.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "false"),
		}
)
@RequiresDialect(value = PostgreSQLDialect.class, comment = "To write a trigger only once")
public class CachingWithTriggerTest {

	private static final String TRIGGER = "begin NEW.lastUpdatedAt = current_timestamp; return NEW; end;";

	@BeforeEach
	public void prepare(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.createNativeQuery( "create function update_ts_func() returns trigger language plpgsql as $$ " + TRIGGER + " $$" )
							.executeUpdate();
					s.createNativeQuery( "create trigger update_ts before insert on TestEntity for each row execute procedure update_ts_func()" )
							.executeUpdate();
				}
		);
	}

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.createNativeQuery( "drop trigger if exists update_ts on TestEntity" )
							.executeUpdate();
					s.createNativeQuery( "drop function if exists update_ts_func()" )
							.executeUpdate();
					s.createQuery( "delete from TestEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testPersistThenRefresh(EntityManagerFactoryScope scope) {
		final long testEntityId = 1L;

		scope.inTransaction(
				entityManager -> {
					TestEntity entity = new TestEntity( testEntityId, "test" );
					entityManager.persist( entity );
					entityManager.flush();
					// No reload happens
					assertThat( entity.lastUpdatedAt ).isNull();
				}
		);
		scope.inTransaction(
				entityManager -> {
					TestEntity entity = entityManager.find( TestEntity.class, testEntityId );
					entityManager.refresh( entity );
					// On refresh, we want the actual data from the database
					assertThat( entity.lastUpdatedAt ).isNotNull();
				}
		);
		scope.inTransaction(
				entityManager -> {
					TestEntity entity = entityManager.find( TestEntity.class, testEntityId );
					// Ensure that we don't get stale data
					assertThat( entity.lastUpdatedAt ).isNotNull();
				}
		);
	}

	@Test
	public void testPersistThenRefreshInTransaction(EntityManagerFactoryScope scope) {
		final long testEntityId = 1L;

		scope.inTransaction(
				entityManager -> {
					TestEntity entity = new TestEntity( testEntityId, "test" );
					entityManager.persist( entity );
					entityManager.flush();
					entityManager.refresh( entity );
					// On refresh, we want the actual data from the database
					assertThat( entity.lastUpdatedAt ).isNotNull();
				}
		);

		scope.inTransaction(
				entityManager -> {
					TestEntity entity = entityManager.find( TestEntity.class, testEntityId );
					// Ensure that we don't get stale data
					assertThat( entity.lastUpdatedAt ).isNotNull();
				}
		);
	}

	@Test
	public void testPersistThenRefreshClearAndQueryInTransaction(EntityManagerFactoryScope scope) {
		final long testEntityId = 1L;

		scope.inTransaction(
				entityManager -> {
					TestEntity entity = new TestEntity( testEntityId, "test" );
					entityManager.persist( entity );
					entityManager.flush();
					entityManager.refresh( entity );
					// On refresh, we want the actual data from the database
					assertThat( entity.lastUpdatedAt ).isNotNull();
					entityManager.clear();

					entity = entityManager.find( TestEntity.class, testEntityId );
					// Ensure that we don't get stale data
					assertThat( entity.lastUpdatedAt ).isNotNull();
				}
		);

		scope.inTransaction(
				entityManager -> {
					TestEntity entity = entityManager.find( TestEntity.class, testEntityId );
					// Ensure that we don't get stale data
					assertThat( entity.lastUpdatedAt ).isNotNull();
				}
		);
	}

	@Entity(name = "TestEntity")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		private Instant lastUpdatedAt;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
