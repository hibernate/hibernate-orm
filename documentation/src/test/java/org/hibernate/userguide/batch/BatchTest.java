/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.batch;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.userguide.util.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BatchTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( BatchTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class,
			Partner.class
		};
	}

	@Test
	public void testScroll() {
		withScroll();
	}

	@Test
	public void testStatelessSession() {
		withStatelessSession();
	}

	@Test
	public void testBulk() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Customer( "Vlad" ) );
			entityManager.persist( new Customer( "Mihalcea" ) );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";
			//tag::batch-bulk-jpql-update-example[]
			int updatedEntities = entityManager.createQuery(
				"update Customer c " +
				"set c.name = :newName " +
				"where c.name = :oldName" )
			.setParameter( "oldName", oldName )
			.setParameter( "newName", newName )
			.executeUpdate();
			//end::batch-bulk-jpql-update-example[]
			assertEquals(1, updatedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Alexandru";
			String newName = "Vlad";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-update-example[]
			int updatedEntities = session.createQuery(
				"update Customer " +
				"set name = :newName " +
				"where name = :oldName" )
			.setParameter( "oldName", oldName )
			.setParameter( "newName", newName )
			.executeUpdate();
			//end::batch-bulk-hql-update-example[]
			assertEquals(1, updatedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-update-version-example[]
			int updatedEntities = session.createQuery(
				"update versioned Customer " +
				"set name = :newName " +
				"where name = :oldName" )
			.setParameter( "oldName", oldName )
			.setParameter( "newName", newName )
			.executeUpdate();
			//end::batch-bulk-hql-update-version-example[]
			assertEquals(1, updatedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String name = "Alexandru";

			//tag::batch-bulk-jpql-delete-example[]
			int deletedEntities = entityManager.createQuery(
				"delete Customer c " +
				"where c.name = :name" )
			.setParameter( "name", name )
			.executeUpdate();
			//end::batch-bulk-jpql-delete-example[]
			assertEquals(1, deletedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String name = "Mihalcea";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-insert-example[]
			int insertedEntities = session.createQuery(
				"insert into Partner (id, name) " +
				"select c.id, c.name " +
				"from Customer c " +
				"where name = :name" )
			.setParameter( "name", name )
			.executeUpdate();
			//end::batch-bulk-hql-insert-example[]
			assertEquals(1, insertedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String name = "Mihalcea";

			Session session = entityManager.unwrap( Session.class );
			//tag::batch-bulk-hql-delete-example[]
			int deletedEntities = session.createQuery(
				"delete Customer " +
				"where name = :name" )
			.setParameter( "name", name )
			.executeUpdate();
			//end::batch-bulk-hql-delete-example[]
			assertEquals(1, deletedEntities);
		} );
	}

	private void withoutBatch() {
		//tag::batch-session-batch-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();

			txn = entityManager.getTransaction();
			txn.begin();

			for ( int i = 0; i < 100_000; i++ ) {
				Customer customer = new Customer( String.format( "Customer %d", i ) );
				entityManager.persist( customer );
			}

			txn.commit();
		} catch (RuntimeException e) {
			if ( txn != null && txn.isActive()) txn.rollback();
				throw e;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-batch-example[]
	}

	private void withBatch() {
		int entityCount = 100;
		//tag::batch-session-batch-insert-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();

			txn = entityManager.getTransaction();
			txn.begin();

			int batchSize = 25;

			for ( int i = 0; i < entityCount; ++i ) {
				Customer customer = new Customer( String.format( "Customer %d", i ) );
				entityManager.persist( customer );

				if ( i % batchSize == 0 ) {
					//flush a batch of inserts and release memory
					entityManager.flush();
					entityManager.clear();
				}
			}

			txn.commit();
		} catch (RuntimeException e) {
			if ( txn != null && txn.isActive()) txn.rollback();
				throw e;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-batch-insert-example[]
	}

	private void withScroll() {
		withBatch();

		//tag::batch-session-scroll-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		ScrollableResults scrollableResults = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();

			txn = entityManager.getTransaction();
			txn.begin();

			int batchSize = 25;

			Session session = entityManager.unwrap( Session.class );

			scrollableResults = session
				.createQuery( "select c from Customer c" )
				.setCacheMode( CacheMode.IGNORE )
				.scroll( ScrollMode.FORWARD_ONLY );

			int count = 0;
			while ( scrollableResults.next() ) {
				Customer customer = (Customer) scrollableResults.get( 0 );
				processCustomer(customer);
				if ( ++count % batchSize == 0 ) {
					//flush a batch of updates and release memory:
					entityManager.flush();
					entityManager.clear();
				}
			}

			txn.commit();
		} catch (RuntimeException e) {
			if ( txn != null && txn.isActive()) txn.rollback();
				throw e;
		} finally {
			if (scrollableResults != null) {
				scrollableResults.close();
			}
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-scroll-example[]
	}

	private void withStatelessSession() {
		withBatch();

		//tag::batch-stateless-session-example[]
		StatelessSession statelessSession = null;
		Transaction txn = null;
		ScrollableResults scrollableResults = null;
		try {
			SessionFactory sessionFactory = entityManagerFactory().unwrap( SessionFactory.class );
			statelessSession = sessionFactory.openStatelessSession();

			txn = statelessSession.getTransaction();
			txn.begin();

			scrollableResults = statelessSession
				.createQuery( "select c from Customer c" )
				.scroll(ScrollMode.FORWARD_ONLY);

			while ( scrollableResults.next() ) {
				Customer customer = (Customer) scrollableResults.get( 0 );
				processCustomer(customer);
				statelessSession.update( customer );
			}

			txn.commit();
		} catch (RuntimeException e) {
			if ( txn != null && txn.getStatus() == TransactionStatus.ACTIVE) txn.rollback();
				throw e;
		} finally {
			if (scrollableResults != null) {
				scrollableResults.close();
			}
			if (statelessSession != null) {
				statelessSession.close();
			}
		}
		//end::batch-stateless-session-example[]
	}

	private void processCustomer(Customer customer) {
		if ( customer.getId() % 1000 == 0 ) {
			log.infof( "Processing [%s]", customer.getName());
		}
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private int version;

		private String name;

		public Customer() {}

		public Customer(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Partner")
	public static class Partner {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private int version;

		private String name;

		public Partner() {}

		public Partner(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
