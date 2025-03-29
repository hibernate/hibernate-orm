/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.Assert.assertEquals;

/**
 * @author Janario Oliveira
 */
public class ManyToManyCollectionCacheEvictionTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Customer.class, Application.class};
	}

	@Before
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@After
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.AUTO_EVICT_COLLECTION_CACHE, true );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, true );
		cfg.setProperty( Environment.USE_QUERY_CACHE, true );
		cfg.setProperty( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Test
	public void testManyToManyPersist() {
		//if an error happen, it will propagate the exception failing the test case
		Session s = openSession();
		s.beginTransaction();

		Application application = new Application();
		s.persist( application );

		Customer customer = new Customer();
		customer.applications.add( application );
		s.persist( customer );

		s.getTransaction().commit();
		s.close();


		s = openSession();

		assertEquals( 1, s.get( Application.class, application.id ).customers.size() );
		assertEquals( 1, s.get( Customer.class, customer.id ).applications.size() );

		s.close();

		s = openSession();
		s.beginTransaction();

		Customer customer2 = new Customer();
		customer2.applications.add( application );
		s.persist( customer2 );

		s.getTransaction().commit();
		s.close();
	}

	@Entity(name = "Customer")
	@Table(name = "customer")
	@Cacheable
	public static class Customer {
		@Id
		@GeneratedValue
		private Integer id;
		@ManyToMany
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Application> applications = new ArrayList<Application>();
	}

	@Entity(name = "Application")
	@Table(name = "application")
	@Cacheable
	public static class Application {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToMany(mappedBy = "applications")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Customer> customers = new ArrayList<Customer>();
	}
}
