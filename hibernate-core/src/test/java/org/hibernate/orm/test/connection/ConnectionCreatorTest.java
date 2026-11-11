package org.hibernate.orm.test.connection;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.BasicConnectionCreator;
import org.hibernate.engine.jdbc.connections.internal.DriverConnectionCreator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.Service;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ConnectionCreatorTest {

	@Test
	@JiraKey(value = "HHH-8621")
	public void testBadUrl() throws Exception {
		CCTStandardServiceRegistryImpl serviceRegistry = CCTStandardServiceRegistryImpl.create(
				true,
				new BootstrapServiceRegistryImpl(),
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyMap()
		);
		try {
			DriverConnectionCreator connectionCreator = new DriverConnectionCreator(
					(Driver) Class.forName( "org.h2.Driver" ).newInstance(),
					serviceRegistry,
					"jdbc:h2:mem:test-bad-urls;nosuchparam=saywhat;DB_CLOSE_ON_EXIT=FALSE",
					new Properties(),
					false,
					null,
					null
			);

			assertThrows( JDBCConnectionException.class, () -> {
						try (Connection conn = connectionCreator.createConnection()) {

						}
					},
					"Expecting the bad Connection URL to cause an exception" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy(serviceRegistry);
		}

	}

	@Test
	@JiraKey(value = "HHH-21108")
	public void testIsolationIsSetAfterDisablingAutoCommit() throws Exception {
		CCTStandardServiceRegistryImpl serviceRegistry = CCTStandardServiceRegistryImpl.create(
				true,
				new BootstrapServiceRegistryImpl(),
				Collections.emptyList(),
				Collections.emptyList(),
				Collections.emptyMap()
		);
		try {
			final List<String> calls = new ArrayList<>();
			final BasicConnectionCreator connectionCreator = new BasicConnectionCreator(
					serviceRegistry,
					"jdbc:test",
					new Properties(),
					false,
					Connection.TRANSACTION_READ_COMMITTED,
					null
			) {
				@Override
				protected Connection makeConnection(String url, Properties connectionProps) {
					return connectionRejectingIsolationChangeInAutoCommit( calls );
				}
			};

			try (Connection conn = connectionCreator.createConnection()) {
				assertFalse( conn.getAutoCommit() );
			}
			assertEquals(
					List.of( "setAutoCommit(false)", "setTransactionIsolation(" + Connection.TRANSACTION_READ_COMMITTED + ")" ),
					calls
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	/**
	 * A connection that, like some JDBC drivers, rejects changing the
	 * transaction isolation level while in auto-commit mode.
	 */
	private static Connection connectionRejectingIsolationChangeInAutoCommit(List<String> calls) {
		final boolean[] autoCommit = { true };
		return (Connection) Proxy.newProxyInstance(
				ConnectionCreatorTest.class.getClassLoader(),
				new Class<?>[] { Connection.class },
				(proxy, method, args) -> switch ( method.getName() ) {
					case "getAutoCommit" -> autoCommit[0];
					case "setAutoCommit" -> {
						autoCommit[0] = (boolean) args[0];
						calls.add( "setAutoCommit(" + args[0] + ")" );
						yield null;
					}
					case "setTransactionIsolation" -> {
						if ( autoCommit[0] ) {
							throw new SQLException( "The transaction isolation level cannot be modified for autocommit mode." );
						}
						calls.add( "setTransactionIsolation(" + args[0] + ")" );
						yield null;
					}
					case "close" -> null;
					case "toString" -> "connection rejecting isolation change in auto-commit mode";
					default -> throw new UnsupportedOperationException( method.getName() );
				}
		);
	}

	private final static class CCTStandardServiceRegistryImpl extends StandardServiceRegistryImpl {

		private CCTStandardServiceRegistryImpl(
				boolean autoCloseRegistry,
				BootstrapServiceRegistry bootstrapServiceRegistry,
				Map<String, Object> configurationValues) {
			super( autoCloseRegistry, bootstrapServiceRegistry, configurationValues );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R extends Service> R getService(@Nonnull Class<R> serviceRole) {
			if ( JdbcServices.class.equals( serviceRole ) ) {
				// return a new, not fully initialized JdbcServicesImpl
				JdbcServicesImpl jdbcServices = new JdbcServicesImpl( this );
				jdbcServices.configure( new HashMap<>() );
				return (R) jdbcServices;
			}
			if ( JdbcEnvironment.class.equals( serviceRole ) ) {
				return (R) new JdbcEnvironmentImpl( this, new H2Dialect() );
			}
			return super.getService( serviceRole );
		}

		public static CCTStandardServiceRegistryImpl create(
				boolean autoCloseRegistry,
				BootstrapServiceRegistry bootstrapServiceRegistry,
				List<StandardServiceInitiator<?>> serviceInitiators,
				List<ProvidedService<?>> providedServices,
				Map<String, Object> configurationValues) {

			CCTStandardServiceRegistryImpl instance = new CCTStandardServiceRegistryImpl( autoCloseRegistry,
					bootstrapServiceRegistry, configurationValues );
			instance.initialize();
			instance.applyServiceRegistrations( serviceInitiators, providedServices );

			return instance;
		}
	}
}
