/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.transactions;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;

/**
 * @author Vlad Mihalcea
 */

@SuppressWarnings("JUnitMalformedDeclaration")
public class TransactionsTest {

	@Test
	@ServiceRegistry(settings = @Setting(name=TRANSACTION_COORDINATOR_STRATEGY, value = "jdbc"))
	public void jdbc(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();

		/*
		//tag::transactions-api-jdbc-example[]
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				// "jdbc" is the default, but for explicitness
				.applySetting(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jdbc")
				.build();

		//end::transactions-api-jdbc-example[]
		*/

		//tag::transactions-api-jdbc-example[]
		Metadata metadata = new MetadataSources(serviceRegistry)
				.addAnnotatedClass(Customer.class)
				.getMetadataBuilder()
				.build();

		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
				.build();

		Session session = sessionFactory.openSession();
		try {
			// calls Connection#setAutoCommit(false) to
			// signal start of transaction
			session.getTransaction().begin();

			session.createMutationQuery("UPDATE customer set NAME = 'Sir. '||NAME")
					.executeUpdate();

			// calls Connection#commit(), if an error
			// happens we attempt a rollback
			session.getTransaction().commit();
		}
		catch (Exception e) {
			// we may need to rollback depending on
			// where the exception happened
			if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE
					|| session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK) {
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
	@ServiceRegistry(settings = @Setting(name=TRANSACTION_COORDINATOR_STRATEGY, value = "jta"))
	public void cmt(ServiceRegistryScope registryScope) {
		StandardServiceRegistry serviceRegistry = registryScope.getRegistry();

		/*
		//tag::transactions-api-cmt-example[]
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta")
				.build();

		//end::transactions-api-cmt-example[]
		*/

		//tag::transactions-api-cmt-example[]
		Metadata metadata = new MetadataSources(serviceRegistry)
				.addAnnotatedClass(Customer.class)
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

			Number customerCount = (Number) session.createSelectionQuery("select count(c) from Customer c").uniqueResult();

			// Since we did not start the transaction (CMT),
			// we also will not end it.  This call essentially
			// no-ops in terms of transaction handling.
			session.getTransaction().commit();
		}
		catch (Exception e) {
			// again, the rollback call here would no-op (aside from
			// marking the underlying CMT transaction for rollback only).
			if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE
					|| session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK) {
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
	@ServiceRegistry(settings = @Setting(name=TRANSACTION_COORDINATOR_STRATEGY, value = "jta"))
	public void bmt(ServiceRegistryScope registryScope) {
		StandardServiceRegistry serviceRegistry = registryScope.getRegistry();

		/*
		//tag::transactions-api-bmt-example[]
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				// "jdbc" is the default, but for explicitness
				.applySetting(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta")
				.build();

		//end::transactions-api-bmt-example[]
		*/

		//tag::transactions-api-bmt-example[]
		Metadata metadata = new MetadataSources(serviceRegistry)
				.addAnnotatedClass(Customer.class)
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

			session.persist(new Customer());
			Customer customer = (Customer) session.createSelectionQuery("select c from Customer c").uniqueResult();

			// calls TM/UT commit method, assuming we are initiator.
			session.getTransaction().commit();
		}
		catch (Exception e) {
			// we may need to rollback depending on
			// where the exception happened
			if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE
					|| session.getTransaction().getStatus() == TransactionStatus.MARKED_ROLLBACK) {
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
