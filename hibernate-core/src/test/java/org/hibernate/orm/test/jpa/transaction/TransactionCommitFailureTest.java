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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.RollbackException;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@BaseUnitTest
@Jpa(
		integrationSettings = {
				@Setting(name = Environment.HBM2DDL_AUTO, value = "create-drop"),
				@Setting(name = Environment.USE_NEW_ID_GENERATOR_MAPPINGS, value = "true")
		},
		nonStringValueSettingProviders = {
				TransactionCommitFailureTest.DialectClassNameProvider.class,
				TransactionCommitFailureTest.ConnectionProviderInstanceProvider.class
		}
)
public class TransactionCommitFailureTest {

	public static final String COMMIT_FAILURE = "Error while committing the transaction";

	private static AtomicBoolean transactionFailureTrigger;
	private static AtomicBoolean connectionIsOpen;
	private static ProxyConnectionProvider connectionProviderInstance;

	static {
		// static variables need to be initialized before the EMF is set up, because they can be referenced during EMF setup via the connection provider.
		transactionFailureTrigger = new AtomicBoolean();
		connectionIsOpen = new AtomicBoolean();

		connectionProviderInstance = new ProxyConnectionProvider();
	}

	@Test
	public void assertConnectionIsReleasedIfCommitFails(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						transactionFailureTrigger.set( true );
						entityManager.getTransaction().commit();
					}
					catch (RollbackException e) {
						assertEquals( COMMIT_FAILURE, e.getLocalizedMessage());
					}
					finally {
						if ( entityManager.getTransaction() != null && entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);

		assertEquals( false, connectionIsOpen.get(), "The connection was not released" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12285")
	public void assertConnectionIsReleasedIfRollbackFails(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								Exception.class,
								() -> {
									entityManager.getTransaction().begin();
									assertEquals( true, connectionIsOpen.get() );
									transactionFailureTrigger.set( true );
									entityManager.getTransaction().rollback();
								},
								"Rollback failure, Exception expected"
						)
		);

		assertEquals( false, connectionIsOpen.get(), "The connection was not released" );
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

	public static class DialectClassNameProvider extends NonStringValueSettingProvider {
		@Override
		public String getKey() {
			return Environment.DIALECT;
		}

		@Override
		public Object getValue() {
			return Dialect.getDialect().getClass().getName();
		}
	}

	public static class ConnectionProviderInstanceProvider extends NonStringValueSettingProvider {
		@Override
		public String getKey() {
			return Environment.CONNECTION_PROVIDER;
		}

		@Override
		public Object getValue() {
			return connectionProviderInstance;
		}
	}
}
