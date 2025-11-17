/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.CustomParameterized;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

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
	private final ConnectionProvider connectionProvider = spy(
			new ConnectionProviderDelegate( ConnectionProviderBuilder.buildConnectionProvider() )
	);

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
		config.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
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
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-13976" ),
			@JiraKey( value = "HHH-14326" )
	} )
	public void testResourcesReleasedThenConnectionClosedThenCommit() throws SQLException, XAException {
		try (SessionImplementor s = (SessionImplementor) openSession()) {
			XAResource transactionSpy = mock( XAResource.class );
			Connection[] connections = new Connection[1];
			Statement statementMock = Mockito.mock( Statement.class );

			TransactionUtil2.inTransaction( s, session -> {
				spyOnTransaction( transactionSpy );

				Thing thing = new Thing();
				thing.setId( 1 );
				session.persist( thing );

				LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();
				logicalConnection.getResourceRegistry().register( statementMock, true );
				connections[0] = logicalConnection.getPhysicalConnection();
			} );

			// Note: all this must happen BEFORE the session is closed;
			// it's particularly important when reusing the session.

			Connection connection = connections[0];

			// Must close the resources, then the connection, then commit
			InOrder inOrder = inOrder( statementMock, connectionProvider, transactionSpy );
			inOrder.verify( statementMock ).close();
			inOrder.verify( connectionProvider ).closeConnection( connection );
			inOrder.verify( transactionSpy ).commit( any(), anyBoolean() );
		}
	}

	@Test
	@JiraKey(value = "HHH-14557")
	public void testResourcesReleasedThenConnectionClosedOnEachRollback() throws SQLException {
		try (SessionImplementor s = (SessionImplementor) openSession()) {
			Connection[] connections = new Connection[1];
			Statement statementMock = Mockito.mock( Statement.class );
			RuntimeException rollbackException = new RuntimeException("Rollback");

			try {
				TransactionUtil2.inTransaction( s, session -> {
					Thing thing = new Thing();
					thing.setId( 1 );
					session.persist( thing );

					LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator().getLogicalConnection();
					logicalConnection.getResourceRegistry().register( statementMock, true );
					connections[0] = logicalConnection.getPhysicalConnection();

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

			Connection connection = connections[0];

			// Must close the resources, then the connection
			InOrder inOrder = inOrder( statementMock, connectionProvider );
			inOrder.verify( statementMock ).close();
			inOrder.verify( connectionProvider ).closeConnection( connection );
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

}
