/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RequiresDialect(H2Dialect.class)
@DomainModel(
		annotatedClasses = {
				AbstractBeforeCompletionReleaseTest.Thing.class
		}
)

@SessionFactory
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public abstract class AbstractBeforeCompletionReleaseTest {

	public static final ConnectionProvider connectionProvider = spy(
			new ConnectionProviderDelegate( ConnectionProviderBuilder.buildConnectionProvider() )
	);

	public static class ConnectionProviderSettingProvider implements SettingProvider.Provider<ConnectionProvider> {
		@Override
		public ConnectionProvider getSetting() {
			return connectionProvider;
		}
	}

	public abstract PhysicalConnectionHandlingMode getConnectionHandlingModeInSessionBuilder();

	@Test
	@JiraKeyGroup(value = {
			@JiraKey(value = "HHH-13976"),
			@JiraKey(value = "HHH-14326")
	})
	public void testResourcesReleasedThenConnectionClosedThenCommit(SessionFactoryScope scope)
			throws SQLException, XAException {
		try (SessionImplementor s = openSession( scope )) {
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
	public void testResourcesReleasedThenConnectionClosedOnEachRollback(SessionFactoryScope scope) throws SQLException {
		try (SessionImplementor s = openSession( scope )) {
			Connection[] connections = new Connection[1];
			Statement statementMock = Mockito.mock( Statement.class );
			RuntimeException rollbackException = new RuntimeException( "Rollback" );

			try {
				TransactionUtil2.inTransaction( s, session -> {
					Thing thing = new Thing();
					thing.setId( 1 );
					session.persist( thing );

					LogicalConnectionImplementor logicalConnection = session.getJdbcCoordinator()
							.getLogicalConnection();
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

	private SessionImplementor openSession(SessionFactoryScope scope) {
		PhysicalConnectionHandlingMode connectionHandlingModeInSessionBuilder = getConnectionHandlingModeInSessionBuilder();
		if ( connectionHandlingModeInSessionBuilder == null ) {
			return scope.getSessionFactory().openSession();
		}
		return scope.getSessionFactory().withOptions()
				.connectionHandlingMode( connectionHandlingModeInSessionBuilder )
				.openSession();
	}

	private void spyOnTransaction(XAResource xaResource) {
		try {
			TestingJtaPlatformImpl.transactionManager().getTransaction().enlistResource( xaResource );
		}
		catch (RollbackException | SystemException e) {
			throw new IllegalStateException( e );
		}
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
