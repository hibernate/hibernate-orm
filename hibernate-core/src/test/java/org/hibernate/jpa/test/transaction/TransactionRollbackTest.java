/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.Session;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class TransactionRollbackTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Shipment.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11407")
	public void checkRollBackTransactionIsExecutedOnceWhenACommitFails() throws Exception {
		EntityManager em = createEntityManager();
		try {
			final Session session = em.unwrap( Session.class );
			final OperationCollectorObserver transactionObserver = new OperationCollectorObserver();
			( (JdbcSessionOwner) session ).getTransactionCoordinator().addObserver( transactionObserver );
			em.getTransaction().begin();

			// given two inserted records
			em.persist( new Shipment( "shipment-1", "INITIAL" ) );
			em.persist( new Shipment( "shipment-2", "INITIAL" ) );

			em.flush();
			em.clear();

			try {
				// when provoking a duplicate-key exception
				em.persist( new Shipment( "shipment-1", "INITIAL" ) );
				em.getTransaction().commit();
				fail( "Expected exception was not raised" );
			}
			catch (Exception e) {
				// Nothing to do
			}

			assertThat( transactionObserver.getUnSuccessfulAfterCompletion(), is( 1 ) );

			em.clear();
			em.getTransaction().begin();

			Shipment shipment = em.find( Shipment.class, "shipment-1" );
			if ( shipment != null ) {
				em.remove( shipment );
			}

			shipment = em.find( Shipment.class, "shipment-2" );
			if ( shipment != null ) {
				em.remove( shipment );
			}

			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
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
