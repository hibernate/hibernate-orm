/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.events;

import java.io.Serializable;
import java.util.Arrays;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.type.Type;

import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class InterceptorTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger LOGGER = Logger.getLogger( InterceptorTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Customer.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Customer( "John Doe" ) );
			Customer customer = new Customer();
			entityManager.persist( customer );
		} );
	}

	@Test
	public void testSessionInterceptor() {
		EntityManagerFactory entityManagerFactory = entityManagerFactory();
		Serializable customerId = 1L;
		//tag::events-interceptors-session-scope-example[]
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		Session session = sessionFactory
			.withOptions()
			.interceptor(new LoggingInterceptor() )
			.openSession();
		session.getTransaction().begin();

		Customer customer = session.get( Customer.class, customerId );
		customer.setName( "Mr. John Doe" );
		//Entity Customer#1 changed from [John Doe, 0] to [Mr. John Doe, 0]

		session.getTransaction().commit();
		//end::events-interceptors-session-scope-example[]
		session.close();
	}

	@Test
	public void testSessionFactoryInterceptor() {

		Serializable customerId = 1L;
		//tag::events-interceptors-session-factory-scope-example[]
		SessionFactory sessionFactory = new MetadataSources( new StandardServiceRegistryBuilder().build() )
			.addAnnotatedClass( Customer.class )
			.getMetadataBuilder()
			.build()
			.getSessionFactoryBuilder()
			.applyInterceptor( new LoggingInterceptor() )
			.build();
		//end::events-interceptors-session-factory-scope-example[]
		Session session = sessionFactory.openSession();
		session.getTransaction().begin();

		Customer customer = session.get( Customer.class, customerId );
		customer.setName( "Mr. John Doe" );
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
	public static class LoggingInterceptor extends EmptyInterceptor {
		@Override
		public boolean onFlushDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
				LOGGER.debugv( "Entity {0}#{1} changed from {2} to {3}",
					entity.getClass().getSimpleName(),
					id,
					Arrays.toString( previousState ),
					Arrays.toString( currentState )
				);
				return super.onFlushDirty( entity, id, currentState,
					previousState, propertyNames, types
			);
		}
	}
	//end::events-interceptors-example[]
}
