/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ondemandload.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Janario Oliveira
 */
public class CacheLazyLoadNoTransTest extends BaseNonConfigCoreFunctionalTestCase {

	@SuppressWarnings("unchecked")
	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		settings.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( Environment.USE_QUERY_CACHE, "true" );
		settings.put( Environment.CACHE_PROVIDER_CONFIG, "true" );
	}

	@Test
	public void hibernateInitialize() {
		Customer customer = new Customer();
		Item item1 = new Item( customer );
		Item item2 = new Item( customer );
		customer.boughtItems.add( item1 );
		customer.boughtItems.add( item2 );
		persist( customer );

		customer = find( Customer.class, customer.id );
		assertFalse( Hibernate.isInitialized( customer.boughtItems ) );
		Hibernate.initialize( customer.boughtItems );
		assertTrue( Hibernate.isInitialized( customer.boughtItems ) );
	}

	@Test
	public void testOneToMany() {
		Customer customer = new Customer();
		Item item1 = new Item( customer );
		Item item2 = new Item( customer );
		customer.boughtItems.add( item1 );
		customer.boughtItems.add( item2 );
		persist( customer );

		//init cache
		assertFalse( isCached( customer.id, Customer.class, "boughtItems" ) );
		customer = find( Customer.class, customer.id );
		assertEquals( 2, customer.boughtItems.size() );

		//read from cache
		assertTrue( isCached( customer.id, Customer.class, "boughtItems" ) );
		customer = find( Customer.class, customer.id );
		assertEquals( 2, customer.boughtItems.size() );
	}

	@Test
	public void testManyToMany() {
		Application application = new Application();
		persist( application );
		Customer customer = new Customer();
		customer.applications.add( application );
		application.customers.add( customer );
		persist( customer );

		//init cache
		assertFalse( isCached( customer.id, Customer.class, "applications" ) );
		assertFalse( isCached( application.id, Application.class, "customers" ) );

		customer = find( Customer.class, customer.id );
		assertEquals( 1, customer.applications.size() );
		application = find( Application.class, application.id );
		assertEquals( 1, application.customers.size() );

		assertTrue( isCached( customer.id, Customer.class, "applications" ) );
		assertTrue( isCached( application.id, Application.class, "customers" ) );

		//read from cache
		customer = find( Customer.class, customer.id );
		assertEquals( 1, customer.applications.size() );
		application = find( Application.class, application.id );
		assertEquals( 1, application.customers.size() );
	}

	private void persist(Object entity) {
		Session session = openSession();
		session.beginTransaction();
		session.persist( entity );
		session.getTransaction().commit();
		session.close();
	}

	private <E> E find(Class<E> entityClass, int id) {
		Session session;
		session = openSession();
		E customer = session.get( entityClass, id );
		session.close();
		return customer;
	}

	private boolean isCached(Serializable id, Class<?> entityClass, String attr) {
		Session session = openSession();
		CollectionPersister persister = sessionFactory().getCollectionPersister( entityClass.getName() + "." + attr );
		CollectionDataAccess cache = persister.getCacheAccessStrategy();
		Object key = cache.generateCacheKey( id, persister, sessionFactory(), session.getTenantIdentifier() );
		Object cachedValue = cache.get( ( (SessionImplementor) session ), key );
		session.close();
		return cachedValue != null;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Application.class, Customer.class, Item.class};
	}

	@Entity
	@Table(name = "application")
	@Cacheable
	public static class Application {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToMany(mappedBy = "applications")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Customer> customers = new ArrayList<>();
	}

	@Entity
	@Table(name = "customer")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Customer {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToMany
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Application> applications = new ArrayList<>();

		@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Item> boughtItems = new ArrayList<>();
	}

	@Entity
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

		protected Item() {
		}

		public Item(Customer customer) {
			this.customer = customer;
		}
	}
}
