/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.orm.test.jpa.SettingsGenerator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@BaseUnitTest
public class TransactionCommitFailureTest {

	public static final String COMMIT_FAILURE = "Error while committing the transaction";

	private static AtomicBoolean transactionFailureTrigger;
	private static AtomicBoolean connectionIsOpen;

	private EntityManagerFactory emf;

	@BeforeEach
	public void setUp() {
		// static variables need to be initialized before the EMF is set up, because they can be referenced during EMF setup via the connection provider.
		transactionFailureTrigger = new AtomicBoolean();
		connectionIsOpen = new AtomicBoolean();

		final Map settings = basicSettings();
		emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
	}

	@AfterEach
	public void tearDown() {
		emf.close();
	}

	@Test
	public void assertConnectionIsReleasedIfCommitFails() {
		EntityManager em = emf.createEntityManager();

		try {
			em.getTransaction().begin();
			// Force connection acquisition
			em.createQuery( "select 1" ).getResultList();
			transactionFailureTrigger.set( true );
			em.getTransaction().commit();
		}
		catch (RollbackException e) {
			assertEquals( COMMIT_FAILURE, e.getLocalizedMessage() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}

		assertEquals( false, connectionIsOpen.get(), "The connection was not released" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12285")
	public void assertConnectionIsReleasedIfRollbackFails() {
		EntityManager em = emf.createEntityManager();
		try {
			em.getTransaction().begin();
			// Force connection acquisition
			em.createQuery( "select 1" ).getResultList();
			assertEquals( true, connectionIsOpen.get() );
			transactionFailureTrigger.set( true );
			em.getTransaction().rollback();
			fail( "Rollback failure, Exception expected" );
		}
		catch (Exception pe) {
			//expected
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}

		assertEquals( false, connectionIsOpen.get(), "The connection was not released" );
	}

	private Map basicSettings() {
		return SettingsGenerator.generateSettings(
				Environment.HBM2DDL_AUTO, "create-drop",
				Environment.DIALECT, DialectContext.getDialect().getClass().getName(),
				Environment.CONNECTION_PROVIDER, ProxyConnectionProvider.class.getName()
		);
	}

	public static class ProxyConnectionProvider extends ConnectionProviderDelegate {

		public ProxyConnectionProvider() {
			setConnectionProvider( SharedDriverManagerConnectionProviderImpl.getInstance() );
		}

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
		public void closeConnection(Connection connection) throws SQLException {
			final ConnectionInvocationHandler handler = (ConnectionInvocationHandler)
					Proxy.getInvocationHandler( connection );
			super.closeConnection( handler.delegate );
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
