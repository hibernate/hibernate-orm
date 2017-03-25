/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.transactions;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class TransactionsTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class
		};
	}

	@Test
	public void jdbc() {
		//tag::transactions-api-jdbc-example[]
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				// "jdbc" is the default, but for explicitness
				.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jdbc" )
				.build();

		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Customer.class )
				.getMetadataBuilder()
				.build();

		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
				.build();

		Session session = sessionFactory.openSession();
		try {
			// calls Connection#setAutoCommit( false ) to
			// signal start of transaction
			session.getTransaction().begin();

			session.createQuery( "UPDATE customer set NAME = 'Sir. '||NAME" )
					.executeUpdate();

			// calls Connection#commit(), if an error
			// happens we attempt a rollback
			session.getTransaction().commit();
		}
		catch ( Exception e ) {
			// we may need to rollback depending on
			// where the exception happened
			if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE
					|| session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
				session.getTransaction().rollback();
			}
			// handle the underlying error
		}
		finally {
			session.close();
			sessionFactory.close();
		}
		//end::transactions-api-jdbc-example[]
	}

	@Test
	public void cmt() {
		//tag::transactions-api-cmt-example[]
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				// "jdbc" is the default, but for explicitness
				.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" )
				.build();

		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Customer.class )
				.getMetadataBuilder()
				.build();

		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
				.build();

		// Note: depending on the JtaPlatform used and some optional settings,
		// the underlying transactions here will be controlled through either
		// the JTA TransactionManager or UserTransaction

		Session session = sessionFactory.openSession();
		try {
			// Since we are in CMT, a JTA transaction would
			// already have been started.  This call essentially
			// no-ops
			session.getTransaction().begin();

			Number customerCount = (Number) session.createQuery( "select count(c) from Customer c" ).uniqueResult();

			// Since we did not start the transaction ( CMT ),
			// we also will not end it.  This call essentially
			// no-ops in terms of transaction handling.
			session.getTransaction().commit();
		}
		catch ( Exception e ) {
			// again, the rollback call here would no-op (aside from
			// marking the underlying CMT transaction for rollback only).
			if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE
					|| session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
				session.getTransaction().rollback();
			}
			// handle the underlying error
		}
		finally {
			session.close();
			sessionFactory.close();
		}
		//end::transactions-api-cmt-example[]
	}

	@Test
	public void bmt() {
		//tag::transactions-api-bmt-example[]
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				// "jdbc" is the default, but for explicitness
				.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" )
				.build();

		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Customer.class )
				.getMetadataBuilder()
				.build();

		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
				.build();

		// Note: depending on the JtaPlatform used and some optional settings,
		// the underlying transactions here will be controlled through either
		// the JTA TransactionManager or UserTransaction

		Session session = sessionFactory.openSession();
		try {
			// Assuming a JTA transaction is not already active,
			// this call the TM/UT begin method.  If a JTA
			// transaction is already active, we remember that
			// the Transaction associated with the Session did
			// not "initiate" the JTA transaction and will later
			// nop-op the commit and rollback calls...
			session.getTransaction().begin();

			session.persist( new Customer(  ) );
			Customer customer = (Customer) session.createQuery( "select c from Customer c" ).uniqueResult();

			// calls TM/UT commit method, assuming we are initiator.
			session.getTransaction().commit();
		}
		catch ( Exception e ) {
			// we may need to rollback depending on
			// where the exception happened
			if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE
					|| session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
				// calls TM/UT commit method, assuming we are initiator;
				// otherwise marks the JTA transaction for rollback only
				session.getTransaction().rollback();
			}
			// handle the underlying error
		}
		finally {
			session.close();
			sessionFactory.close();
		}
		//end::transactions-api-bmt-example[]
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
	}
}
