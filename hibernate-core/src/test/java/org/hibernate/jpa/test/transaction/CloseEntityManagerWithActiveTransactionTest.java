/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10942")
public class CloseEntityManagerWithActiveTransactionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.TRANSACTION_TYPE, "JTA" );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class
		};
	}

	@Test
	public void testCloseWithAnActiveTransaction() throws Exception {
		Book book = new Book();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			em.persist( book );
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if(transactionManager.getTransaction().getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
		em = getOrCreateEntityManager();
		try {
			final List results = em.createQuery( "from Book" ).getResultList();
			assertThat( results.size(), is( 1 ) );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11099")
	public void testCommitReleasesLogicalConnection() throws Exception {
		Book book = new Book();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			em.persist( book );
			final SessionImpl session =(SessionImpl) em.unwrap( Session.class );
			final JdbcCoordinatorImpl jdbcCoordinator = (JdbcCoordinatorImpl)session.getJdbcCoordinator();
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			assertThat( "The logical connection is still open after commit",jdbcCoordinator.getLogicalConnection().isOpen(),is(false));
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if(transactionManager.getTransaction().getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
	}
}
