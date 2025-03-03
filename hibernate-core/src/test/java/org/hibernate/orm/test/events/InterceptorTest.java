/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import java.io.Serializable;
import java.util.Arrays;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.type.Type;

import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class InterceptorTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger LOGGER = Logger.getLogger(InterceptorTest.class);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Customer.class
		};
	}

	@Before
	public void init() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.persist(new Customer("John Doe"));
			Customer customer = new Customer();
			entityManager.persist(customer);
		});
	}

	@Test
	public void testSessionInterceptor() {
		EntityManagerFactory entityManagerFactory = entityManagerFactory();
		Serializable customerId = 1L;
		//tag::events-interceptors-session-scope-example[]
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		Session session = sessionFactory
			.withOptions()
			.interceptor(new LoggingInterceptor())
			.openSession();
		session.getTransaction().begin();

		Customer customer = session.get(Customer.class, customerId);
		customer.setName("Mr. John Doe");
		//Entity Customer#1 changed from [John Doe, 0] to [Mr. John Doe, 0]

		session.getTransaction().commit();
		//end::events-interceptors-session-scope-example[]
		session.close();
	}

	@Test
	public void testSessionFactoryInterceptor() {

		Serializable customerId = 1L;
		SessionFactory sessionFactory = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
		/*
		//tag::events-interceptors-session-factory-scope-example[]
		SessionFactory sessionFactory = new MetadataSources(new StandardServiceRegistryBuilder().build())
		//end::events-interceptors-session-factory-scope-example[]
		*/
		//tag::events-interceptors-session-factory-scope-example[]
			.addAnnotatedClass(Customer.class)
			.getMetadataBuilder()
			.build()
			.getSessionFactoryBuilder()
			.applyInterceptor(new LoggingInterceptor())
			.build();
		//end::events-interceptors-session-factory-scope-example[]
		Session session = sessionFactory.openSession();
		session.getTransaction().begin();

		Customer customer = session.get(Customer.class, customerId);
		customer.setName("Mr. John Doe");
		//Entity Customer#1 changed from [John Doe, 0] to [Mr. John Doe, 0]
		session.getTransaction().commit();
		session.close();
		sessionFactory.close();
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Customer() {
		}

		public Customer(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	//tag::events-interceptors-example[]
	public static class LoggingInterceptor implements Interceptor {
		@Override
		public boolean onFlushDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
				LOGGER.debugv("Entity {0}#{1} changed from {2} to {3}",
					entity.getClass().getSimpleName(),
					id,
					Arrays.toString(previousState),
					Arrays.toString(currentState)
				);
				return Interceptor.super.onFlushDirty(entity, id, currentState,
					previousState, propertyNames, types
			);
		}
	}
	//end::events-interceptors-example[]
}
