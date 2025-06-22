/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@Jpa(annotatedClasses = {
		TransactionRollbackTest.Shipment.class
})
public class TransactionRollbackTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-11407")
	public void checkRollBackTransactionIsExecutedOnceWhenACommitFails(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final Session session = entityManager.unwrap( Session.class );
					final OperationCollectorObserver transactionObserver = new OperationCollectorObserver();
					( (JdbcSessionOwner) session ).getTransactionCoordinator().addObserver( transactionObserver );
					try {
						entityManager.getTransaction().begin();

						// given two inserted records
						entityManager.persist( new Shipment( "shipment-1", "INITIAL" ) );
						entityManager.persist( new Shipment( "shipment-2", "INITIAL" ) );

						entityManager.flush();
						entityManager.clear();

						Assertions.assertThrows(
								Exception.class,
								() -> {
									// when provoking a duplicate-key exception
									entityManager.persist( new Shipment( "shipment-1", "INITIAL" ) );
									entityManager.getTransaction().commit();
								},
								"Expected exception was not raised"
						);

						assertThat( transactionObserver.getUnSuccessfulAfterCompletion(), is( 1 ) );

						entityManager.clear();
						entityManager.getTransaction().begin();

						Shipment shipment = entityManager.find( Shipment.class, "shipment-1" );
						if ( shipment != null ) {
							entityManager.remove( shipment );
						}

						shipment = entityManager.find( Shipment.class, "shipment-2" );
						if ( shipment != null ) {
							entityManager.remove( shipment );
						}

						entityManager.getTransaction().commit();
					}
					catch (Exception | AssertionError e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						if ( e instanceof AssertionError ) {
							throw (AssertionError) e;
						}
					}
				}
		);
	}

	@Entity(name = "Shipment")
	public class Shipment {

		@Id
		private String id;

		@Version
		private long version;

		private String state;

		Shipment() {
		}

		public Shipment(String id, String state) {
			this.id = id;
			this.state = state;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

	}

	private class OperationCollectorObserver implements TransactionObserver {
		int unSuccessfulAfterCompletion;

		@Override
		public void afterBegin() {
			// Nothing to do
		}

		@Override
		public void beforeCompletion() {
			// Nothing to do
		}

		@Override
		public void afterCompletion(boolean successful, boolean delayed) {
			if ( !successful ) {
				unSuccessfulAfterCompletion++;
			}
		}

		public int getUnSuccessfulAfterCompletion() {
			return unSuccessfulAfterCompletion;
		}
	}
}
