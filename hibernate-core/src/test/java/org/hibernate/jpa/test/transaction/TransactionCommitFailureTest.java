/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.SettingsGenerator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class TransactionCommitFailureTest extends BaseUnitTestCase {

	public static final String COMMIT_FAILURE = "Error while committing the transaction";

	private static AtomicBoolean transactionFailureTrigger;
	private static AtomicBoolean connectionIsOpen;

	private EntityManagerFactory emf;

	@Before
	public void setUp() {
		// static variables need to be initialized before the EMF is set up, because they can be referenced during EMF setup via the connection provider.
		transactionFailureTrigger = new AtomicBoolean();
		connectionIsOpen = new AtomicBoolean();
		
		final Map settings = basicSettings();
		emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
	}

	@After
	public void tearDown() {
		emf.close();
	}

	@Test
	public void assertConnectionIsReleasedIfCommitFails() {
		EntityManager em = emf.createEntityManager();

		try {
			em.getTransaction().begin();
			transactionFailureTrigger.set( true );
			em.getTransaction().commit();
		}
		catch (RollbackException e) {
			assertEquals( COMMIT_FAILURE, e.getLocalizedMessage());
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}

		assertEquals( "The connection was not released", false, connectionIsOpen.get() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12285")
	public void assertConnectionIsReleasedIfRollbackFails() {
		EntityManager em = emf.createEntityManager();
		try {
			em.getTransaction().begin();
			assertEquals( true, connectionIsOpen.get() );
			transactionFailureTrigger.set( true );
			em.getTransaction().rollback();
			fail( "Rollback failure, Exception expected" );
		}
		catch (Exception pe) {
			//expected
		}
		finally {
			em.close();
		}

		assertEquals( "The connection was not released", false, connectionIsOpen.get() );
	}

	private Map basicSettings() {
		return SettingsGenerator.generateSettings(
				Environment.HBM2DDL_AUTO, "create-drop",
				Environment.USE_NEW_ID_GENERATOR_MAPPINGS, "true",
				Environment.DIALECT, Dialect.getDialect().getClass().getName(),
				Environment.CONNECTION_PROVIDER, ProxyConnectionProvider.class.getName()
		);
	}

	public static class ProxyConnectionProvider extends DriverManagerConnectionProviderImpl {

		@Override
		public Connection getConnection() throws SQLException {
			Connection delegate = super.getConnection();
			connectionIsOpen.set( true );
			return (Connection) Proxy.newProxyInstance(
					this.getClass().getClassLoader(),
					new Class[] { Connection.class },
					new ConnectionInvocationHandler( delegate )
			);
		}

		@Override
		public void closeConnection(Connection conn) throws SQLException {
			super.closeConnection( conn );
			connectionIsOpen.set( false );
		}
	}

	private static class ConnectionInvocationHandler implements InvocationHandler {

		private final Connection delegate;

		public ConnectionInvocationHandler(Connection delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ( "commit".equals( method.getName() ) ) {
				if ( transactionFailureTrigger.get() ) {
					throw new SQLException( COMMIT_FAILURE );
				}
			}
			else if ( "rollback".equals( method.getName() ) ) {
				if ( transactionFailureTrigger.get() ) {
					transactionFailureTrigger.set( false );
					throw new SQLException( "Rollback failed!" );
				}
			}
			return method.invoke( delegate, args );
		}
	}

}
