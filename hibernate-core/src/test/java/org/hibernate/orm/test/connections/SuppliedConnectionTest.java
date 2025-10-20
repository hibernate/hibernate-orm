/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of SuppliedConnectionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@SessionFactory(
		exportSchema = false
)
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = Environment.CONNECTION_HANDLING,
						provider = SuppliedConnectionTest.ConnectionHandlingProvider.class
				),
				@SettingProvider(
						settingName = Environment.CONNECTION_PROVIDER,
						provider = SuppliedConnectionTest.ConnectionProviderProvider.class
				),
				@SettingProvider(
						settingName = Environment.USE_SCROLLABLE_RESULTSET,
						provider = SuppliedConnectionTest.UseScrollableResultSetProvider.class
				),
		}
)
public class SuppliedConnectionTest extends ConnectionManagementTestCase {
	private static ConnectionProvider cp = ConnectionProviderBuilder.buildConnectionProvider();
	private Connection connectionUnderTest;

	public static class ConnectionHandlingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD.toString();
		}
	}

	public static class ConnectionProviderProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return UserSuppliedConnectionProviderImpl.class.getName();
		}
	}

	public static class UseScrollableResultSetProvider implements SettingProvider.Provider<Boolean> {
		@Override
		public Boolean getSetting() {
			try {
				Connection connection = cp.getConnection();
				try {
					return connection.getMetaData()
							.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
				}
				finally {
					cp.closeConnection( connection );
				}
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		}
	}

	@BeforeAll
	protected void prepareTest(SessionFactoryScope scope) throws Exception {

		try {
			Connection conn = cp.getConnection();
			ServiceRegistryImplementor serviceRegistry = scope.getSessionFactory().getServiceRegistry();

			try {
				final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl( serviceRegistry, conn ),
						true
				);
				new SchemaCreatorImpl( serviceRegistry ).doCreation(
						scope.getMetadataImplementor(),
						false,
						target
				);
			}
			finally {
				cp.closeConnection( conn );
			}
		}
		catch (Throwable ignore) {
		}
	}


	@AfterAll
	@SuppressWarnings("unused")
	private void releaseConnectionProvider(SessionFactoryScope scope) {
		try {
			Connection conn = cp.getConnection();
			ServiceRegistryImplementor serviceRegistry = scope.getSessionFactory().getServiceRegistry();

			try {
				final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry,
								conn
						),
						true
				);
				new SchemaDropperImpl( serviceRegistry ).doDrop( scope.getMetadataImplementor(), false, target );
			}
			finally {
				cp.closeConnection( conn );
			}
		}
		catch (Throwable ignore) {
		}
		try {
			if ( cp instanceof Stoppable ) {
				((Stoppable) cp).stop();
			}
			cp = null;
		}
		catch (Throwable ignore) {
		}
	}

	@Override
	protected Session getSessionUnderTest(SessionFactoryScope scope) throws Throwable {
		connectionUnderTest = cp.getConnection();
		Session session = scope.getSessionFactory().withOptions().connection( connectionUnderTest ).openSession();
		session.beginTransaction();
		return session;
	}

	@Override
	protected void reconnect(Session session) {
		((SessionImplementor) session).getJdbcCoordinator().getLogicalConnection()
				.manualReconnect( connectionUnderTest );
	}

	@Override
	protected void done() throws Throwable {
		cp.closeConnection( connectionUnderTest );
	}

}
