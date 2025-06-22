/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.syncSpace;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;

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
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests of how sync-spaces for a native query affect caching
 *
 * @author Samuel Fung
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				NativeQuerySyncSpaceCachingTest.Customer.class,
				NativeQuerySyncSpaceCachingTest.Address.class
		}
)
@SessionFactory()
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
)
public class NativeQuerySyncSpaceCachingTest {

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer customer = new Customer( 1, "Samuel" );
					session.persist( customer );
				}
		);
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	public void testSelectAnotherEntityWithNoSyncSpaces(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();

		scope.inSession(
				session ->
						session.createNativeQuery( "select * from Address" ).list()
		);

		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();
	}

	@Test
	public void testUpdateAnotherEntityWithNoSyncSpaces(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();

		scope.inTransaction(
				session ->
						session.createNativeQuery( "update Address set id = id" ).executeUpdate()
		);

		// NOTE false here because executeUpdate is different than selects
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isFalse();
	}

	@Test
	public void testUpdateAnotherEntityWithSyncSpaces(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();

		scope.inTransaction(
				session ->
						session.createNativeQuery( "update Address set id = id" )
								.addSynchronizedEntityClass( Address.class )
								.executeUpdate()
		);

		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();
	}

	@Test
	public void testSelectCachedEntityWithNoSyncSpaces(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();

		scope.inSession(
				session ->
						session.createNativeQuery( "select * from Customer" ).list()
		);

		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();
	}

	@Test
	public void testUpdateCachedEntityWithNoSyncSpaces(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();

		scope.inTransaction(
				session ->
						session.createNativeQuery( "update Customer set id = id" ).executeUpdate()

		);

		// NOTE false here because executeUpdate is different than selects
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isFalse();
	}

	@Test
	public void testUpdateCachedEntityWithSyncSpaces(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Customer.class, 1 ) ).isTrue();

		scope.inTransaction(
				session ->
						session.createNativeQuery( "update Customer set id = id" )
								.addSynchronizedEntityClass( Customer.class )
								.executeUpdate()
		);

		assertThat( cache.containsEntity( Customer.class, 1 ) ).isFalse();
	}

	@Entity(name = "Customer")
	@Table(name = "Customer")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class Customer {
		@Id
		private int id;

		private String name;

		public Customer() {
		}

		public Customer(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Address")
	@Table(name = "Address")
	public static class Address {
		@Id
		private int id;
		private String text;

		public Address() {
		}

		public Address(int id, String text) {
			this.id = id;
			this.text = text;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
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
