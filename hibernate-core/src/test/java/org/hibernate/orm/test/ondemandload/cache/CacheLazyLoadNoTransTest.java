/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondemandload.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Janario Oliveira
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"),
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true")
		},
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsBagProvider.class
		)
)
@DomainModel(
		annotatedClasses = {
				CacheLazyLoadNoTransTest.Application.class,
				CacheLazyLoadNoTransTest.Customer.class,
				CacheLazyLoadNoTransTest.Item.class
		}
)
@SessionFactory
public class CacheLazyLoadNoTransTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void hibernateInitialize(SessionFactoryScope scope) {
		Customer customer = new Customer();
		Item item1 = new Item( customer );
		Item item2 = new Item( customer );
		customer.boughtItems.add( item1 );
		customer.boughtItems.add( item2 );
		persist( customer, scope );

		customer = find( Customer.class, customer.id, scope );
		assertFalse( Hibernate.isInitialized( customer.boughtItems ) );
		Hibernate.initialize( customer.boughtItems );
		assertTrue( Hibernate.isInitialized( customer.boughtItems ) );
	}

	@Test
	public void testOneToMany(SessionFactoryScope scope) {
		Customer customer = new Customer();
		Item item1 = new Item( customer );
		Item item2 = new Item( customer );
		customer.boughtItems.add( item1 );
		customer.boughtItems.add( item2 );
		persist( customer, scope );

		//init cache
		assertFalse( isCached( customer.id, Customer.class, "boughtItems", scope ) );
		customer = find( Customer.class, customer.id, scope );
		assertThat( customer.boughtItems.size(), is( 2 ) );

		//read from cache
		assertTrue( isCached( customer.id, Customer.class, "boughtItems", scope ) );
		customer = find( Customer.class, customer.id, scope );
		assertThat( customer.boughtItems.size(), is( 2 ) );
	}

	@Test
	public void testManyToMany(SessionFactoryScope scope) {
		Application application = new Application();
		persist( application, scope );
		Customer customer = new Customer();
		customer.applications.add( application );
		application.customers.add( customer );
		persist( customer, scope );

		//init cache
		assertFalse( isCached( customer.id, Customer.class, "applications", scope ) );
		assertFalse( isCached( application.id, Application.class, "customers", scope ) );

		customer = find( Customer.class, customer.id, scope );
		assertThat( customer.applications.size(), is( 1 ) );
		application = find( Application.class, application.id, scope );
		assertThat( application.customers.size(), is( 1 ) );

		assertTrue( isCached( customer.id, Customer.class, "applications", scope ) );
		assertTrue( isCached( application.id, Application.class, "customers", scope ) );

		//read from cache
		customer = find( Customer.class, customer.id, scope );
		assertThat( customer.applications.size(), is( 1 ) );
		application = find( Application.class, application.id, scope );
		assertThat( application.customers.size(), is( 1 ) );
	}

	private void persist(Object entity, SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.persist( entity )
		);
	}

	private <E> E find(Class<E> entityClass, int id, SessionFactoryScope scope) {
		return scope.fromSession(
				session ->
						session.get( entityClass, id )
		);
	}

	private boolean isCached(Serializable id, Class<?> entityClass, String attr, SessionFactoryScope scope) {
		Object value = scope.fromSession(
				session -> {
					final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
					CollectionPersister persister = sessionFactory.getMappingMetamodel().getCollectionDescriptor( entityClass.getName() + "." + attr);
					CollectionDataAccess cache = persister.getCacheAccessStrategy();
					Object key = cache.generateCacheKey( id, persister, sessionFactory, session.getTenantIdentifier() );
					Object cachedValue = cache.get( session, key );
					return cachedValue;
				}
		);

		return value != null;
	}


	@Entity(name = "Application")
	@Table(name = "application")
	@Cacheable
	public static class Application {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToMany(mappedBy = "applications")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Customer> customers = new ArrayList<>();
	}

	@Entity(name = "Customer")
	@Table(name = "customer")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Customer {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToMany
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Application> applications = new ArrayList<>();

		@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Item> boughtItems = new ArrayList<>();
	}

	@Entity(name = "Item")
	@Table(name = "item")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Item {
		@Id
		@GeneratedValue
		private Integer id;
		@ManyToOne
		@JoinColumn(name = "customer_id")
		private Customer customer;

		private String name;

		protected Item() {
		}

		public Item(Customer customer) {
			this.customer = customer;
		}
	}
}
