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
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.SettingsGenerator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TransactionCommitFailureTest {

    public static final String COMMIT_FAILURE = "Commit failed!";

    private static final AtomicBoolean transactionFailureTrigger = new AtomicBoolean( false );

    @Test
    public void testConfiguredInterceptor() {
		Map settings = basicSettings();
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            transactionFailureTrigger.set( true );
            em.getTransaction().commit();
        }
		catch (RollbackException e) {
			assertEquals( COMMIT_FAILURE, e.getCause().getMessage() );
		}
		finally {
            if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }

    protected Map basicSettings() {
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
			return (Connection) Proxy.newProxyInstance(
					this.getClass().getClassLoader(),
					new Class[]{Connection.class},
					new ConnectionInvocationHandler(delegate));
		}
	}

    private static class ConnectionInvocationHandler implements InvocationHandler {

        private final Connection delegate;

        public ConnectionInvocationHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if("commit".equals( method.getName() )) {
                if ( transactionFailureTrigger.get() ) {
                    throw new PersistenceException( COMMIT_FAILURE );
                }
            }
            else if("rollback".equals( method.getName() )) {
                if ( transactionFailureTrigger.get() ) {
                    transactionFailureTrigger.set( false );
                    throw new PersistenceException( "Rollback failed!" );
                }
            }
            return method.invoke(delegate, args);
        }
    }

}
