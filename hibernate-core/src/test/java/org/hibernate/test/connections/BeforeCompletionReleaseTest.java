/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.CustomParameterized;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Luis Barreiro
 */
@RequiresDialect( H2Dialect.class )
@RunWith(CustomParameterized.class)
public class BeforeCompletionReleaseTest extends BaseEntityManagerFunctionalTestCase {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> params() {
        return Arrays.asList( new Object[][] {
                {
                        "Setting connection handling mode from properties",
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION,
                        null
                },
                {
                        "Setting connection handling mode through SessionBuilder",
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT,
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION
                }
        } );
    }

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

    private final PhysicalConnectionHandlingMode connectionHandlingModeInProperties;
    private final PhysicalConnectionHandlingMode connectionHandlingModeInSessionBuilder;

    public BeforeCompletionReleaseTest(
            String ignoredTestLabel, PhysicalConnectionHandlingMode connectionHandlingModeInProperties,
            PhysicalConnectionHandlingMode connectionHandlingModeInSessionBuilder) {
        this.connectionHandlingModeInProperties = connectionHandlingModeInProperties;
        this.connectionHandlingModeInSessionBuilder = connectionHandlingModeInSessionBuilder;
    }

    @Override
    protected Map getConfig() {
        Map config = super.getConfig();
        TestingJtaBootstrap.prepare( config );
        config.put( AvailableSettings.CONNECTION_PROVIDER, new ConnectionProviderDecorator() );
        if ( connectionHandlingModeInProperties != null ) {
            config.put( AvailableSettings.CONNECTION_HANDLING, connectionHandlingModeInProperties );
        }
        return config;
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Thing.class };
    }

    @Test
    @TestForIssue(jiraKey = {"HHH-13976", "HHH-14326"})
    public void testResourcesReleasedThenConnectionClosedThenCommit() throws SQLException, XAException {
        try (SessionImplementor s = (SessionImplementor) openSession()) {
            XAResource transactionSpy = mock( XAResource.class );
            Connection[] connectionSpies = new Connection[1];
            Statement statementMock = Mockito.mock( Statement.class );

            TransactionUtil2.inTransaction( s, session -> {
                spyOnTransaction( transactionSpy );

                Thing thing = new Thing();
                thing.setId( 1 );
                session.persist( thing );

                LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();
                logicalConnection.getResourceRegistry().register( statementMock, true );
                connectionSpies[0] = logicalConnection.getPhysicalConnection();
            } );

            // Note: all this must happen BEFORE the session is closed;
            // it's particularly important when reusing the session.

            Connection connectionSpy = connectionSpies[0];

            // Must close the resources, then the connection, then commit
            InOrder inOrder = inOrder( statementMock, connectionSpy, transactionSpy );
            inOrder.verify( statementMock ).close();
            inOrder.verify( connectionSpy ).close();
            inOrder.verify( transactionSpy ).commit( any(), anyBoolean() );
        }
    }

    @Test
    @TestForIssue(jiraKey = {"HHH-14557"})
    public void testResourcesReleasedThenConnectionClosedOnEachRollback() throws SQLException {
        try (SessionImplementor s = (SessionImplementor) openSession()) {
            Connection[] connectionSpies = new Connection[1];
            Statement statementMock = Mockito.mock( Statement.class );
            RuntimeException rollbackException = new RuntimeException("Rollback");

            try {
                TransactionUtil2.inTransaction( s, session -> {
                    Thing thing = new Thing();
                    thing.setId( 1 );
                    session.persist( thing );

                    LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();
                    logicalConnection.getResourceRegistry().register( statementMock, true );
                    connectionSpies[0] = logicalConnection.getPhysicalConnection();

                    throw rollbackException;
                } );
            }
            catch (RuntimeException e) {
                if ( e != rollbackException ) {
                    throw e;
                }
                // Else: ignore, that was expected.
            }

            // Note: all this must happen BEFORE the session is closed;
            // it's particularly important when reusing the session.

            Connection connectionSpy = connectionSpies[0];

            // Must close the resources, then the connection
            InOrder inOrder = inOrder( statementMock, connectionSpy );
            inOrder.verify( statementMock ).close();
            inOrder.verify( connectionSpy ).close();
            // We don't check the relative ordering of the rollback here,
            // because unfortunately we know it's wrong:
            // we don't get a "before transaction completion" event for rollbacks,
            // so in the case of rollbacks the closing always happen after transaction completion.
        }
    }

    private void spyOnTransaction(XAResource xaResource) {
        try {
            TestingJtaPlatformImpl.transactionManager().getTransaction().enlistResource( xaResource );
        }
        catch (RollbackException | SystemException e) {
            throw new IllegalStateException( e );
        }
    }

    private Session openSession() {
        return connectionHandlingModeInSessionBuilder == null
                ? entityManagerFactory().openSession()
                : entityManagerFactory().withOptions().connectionHandlingMode( connectionHandlingModeInSessionBuilder )
                .openSession();
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
            return spy( dataSource.getConnection() );
        }

        @Override
        public void closeConnection(Connection connection) throws SQLException {
            connection.close();
        }
    }
}

