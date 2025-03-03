/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;

/**
 * Implementation of SuppliedConnectionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class SuppliedConnectionTest extends ConnectionManagementTestCase {
	private ConnectionProvider cp;
	private Connection connectionUnderTest;

	@BeforeClassOnce
	@SuppressWarnings("unused")
	private void prepareConnectionProvider() {
		cp = ConnectionProviderBuilder.buildConnectionProvider();
	}

	@AfterClassOnce
	@SuppressWarnings("unused")
	private void releaseConnectionProvider() {
		try {
			if ( cp instanceof Stoppable ) {
					( ( Stoppable ) cp ).stop();
			}
			cp = null;
		}
		catch( Throwable ignore ) {
		}
	}

	@Override
	protected Session getSessionUnderTest() throws Throwable {
		connectionUnderTest = cp.getConnection();
		Session session = sessionFactory().withOptions().connection( connectionUnderTest ).openSession();
		session.beginTransaction();
		return session;
	}

	@Override
	protected void reconnect(Session session) {
		((SessionImplementor)session).getJdbcCoordinator().getLogicalConnection().manualReconnect( connectionUnderTest );
	}

	@Override
	protected void done() throws Throwable {
		cp.closeConnection( connectionUnderTest );
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );

		settings.put( Environment.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD.toString() );
		settings.put( Environment.CONNECTION_PROVIDER, UserSuppliedConnectionProviderImpl.class.getName() );

		Connection connection;
		try {
			connection = cp.getConnection();
			try {
				boolean supportsScroll = connection.getMetaData().supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
				settings.put( Environment.USE_SCROLLABLE_RESULTSET, "" + supportsScroll );
			}
			finally {
				cp.closeConnection( connection );
			}
		}
		catch (SQLException ignore) {
		}
	}

	@Override
	public boolean createSchema() {
		return false;
	}

	@Override
	public boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();

		try {
			Connection conn = cp.getConnection();

			try {
				final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl( serviceRegistry(), conn ),
						true
				);
				new SchemaCreatorImpl( serviceRegistry() ).doCreation(
						metadata(),
						false,
						target
				);
			}
			finally {
				cp.closeConnection( conn );
			}
		}
		catch( Throwable ignore ) {
		}

	}

	@Override
	protected void cleanupTest() throws Exception {
		try {
			Connection conn = cp.getConnection();

			try {
				final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
						new DdlTransactionIsolatorTestingImpl(
								serviceRegistry(),
								conn
						),
						true
				);
				new SchemaDropperImpl( serviceRegistry() ).doDrop( metadata(), false, target );
			}
			finally {
				cp.closeConnection( conn );
			}
		}
		catch( Throwable ignore ) {
		}

		super.cleanupTest();
	}
}
