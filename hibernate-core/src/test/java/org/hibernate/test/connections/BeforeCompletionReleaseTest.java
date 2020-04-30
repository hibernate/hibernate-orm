/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.connections;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Luis Barreiro
 */
@RequiresDialect( H2Dialect.class )
public class BeforeCompletionReleaseTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Map getConfig() {
        Map config = super.getConfig();
        TestingJtaBootstrap.prepare( config );
        config.put( AvailableSettings.CONNECTION_PROVIDER, new ConnectionProviderDecorator() );
        config.put( AvailableSettings.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION );
        return config;
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Thing.class };
    }

    @Test
    public void testConnectionAcquisitionCount() {
        TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
            Thing thing = new Thing();
            thing.setId( 1 );
            entityManager.persist( thing );
        });
    }

    // --- //

    @Entity(name = "Thing")
    @Table(name = "Thing")
    public static class Thing {

        @Id
        public Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    // --- //

    public static class ConnectionProviderDecorator extends UserSuppliedConnectionProviderImpl {

        private final ConnectionProvider dataSource;

        public ConnectionProviderDecorator() {
            this.dataSource = ConnectionProviderBuilder.buildConnectionProvider();
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = dataSource.getConnection();

            try {
                Transaction tx = TestingJtaPlatformImpl.transactionManager().getTransaction();
                if ( tx != null) {
                    tx.enlistResource( new XAResource() {

                        @Override public void commit(Xid xid, boolean onePhase) {
                            try {
                                assertTrue( "Connection should be closed prior to commit", connection.isClosed() );
                            } catch ( SQLException e ) {
                                fail( "Unexpected SQLException: " + e.getMessage() );
                            }
                        }

                        @Override public void end(Xid xid, int flags) {
                        }

                        @Override public void forget(Xid xid)  {
                        }

                        @Override public int getTransactionTimeout() {
                            return 0;
                        }

                        @Override public boolean isSameRM(XAResource xares) {
                            return false;
                        }

                        @Override public int prepare(Xid xid) {
                            return 0;
                        }

                        @Override public Xid[] recover(int flag) {
                            return new Xid[0];
                        }

                        @Override public void rollback(Xid xid) {
                            try {
                                assertTrue( "Connection should be closed prior to rollback", connection.isClosed() );
                            } catch ( SQLException e ) {
                                fail( "Unexpected SQLException: " + e.getMessage() );
                            }
                        }

                        @Override public boolean setTransactionTimeout(int seconds) {
                            return false;
                        }

                        @Override public void start(Xid xid, int flags) {
                        }
                    });
                }
            } catch ( SystemException | RollbackException e ) {
                fail( e.getMessage() );
            }
            return connection;
        }

        @Override
        public void closeConnection(Connection connection) throws SQLException {
            connection.close();
        }
    }
}
