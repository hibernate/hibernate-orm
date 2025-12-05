/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Janario Oliveira
 */
@DomainModel(
		annotatedClasses = {
				ManyToManyCollectionCacheEvictionTest.Customer.class,
				ManyToManyCollectionCacheEvictionTest.Application.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.AUTO_EVICT_COLLECTION_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.DEFAULT_LIST_SEMANTICS, value = "bag"), // CollectionClassification.BAG
		}
)
@SessionFactory
public class ManyToManyCollectionCacheEvictionTest {

	@BeforeEach
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@AfterEach
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@Test
	public void testManyToManyPersist(SessionFactoryScope scope) {
		//if an error happen, it will propagate the exception failing the test case
		Application application = scope.fromTransaction( s -> {
			Application a = new Application();
			s.persist( a );
			return a;
		} );

		Customer customer = scope.fromTransaction( s -> {
			Customer c = new Customer();
			c.applications.add( application );
			s.persist( c );
			return c;
		} );

		scope.inSession( s -> {
			assertEquals( 1, s.get( Application.class, application.id ).customers.size() );
			assertEquals( 1, s.get( Customer.class, customer.id ).applications.size() );
		} );


		scope.inTransaction( s -> {
			Customer customer2 = new Customer();
			customer2.applications.add( application );
			s.persist( customer2 );
		});
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
