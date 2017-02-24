/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class DelayerConnectionProviderTest extends BaseUnitTestCase {

	@Test(expected = GenericJDBCException.class)
	public void testNonDelayedConnectionProvider() {
		doTestSessionFactoryCreation(
				b -> {
					b.applySetting( AvailableSettings.CONNECTION_PROVIDER, DBNotPresentSimulatorConnectionProvider.class )
						.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
				},
				metadataSources -> metadataSources.addAnnotatedClass( DCPTEntity.class ),
				Metadata::buildSessionFactory,
				(sessionFactory, serviceRegistry) -> {}
		);
	}

	@Test
	public void testDelayedConnectionProvider() {
		doTestSessionFactoryCreation(
				b -> {
					b.applySetting( AvailableSettings.CONNECTION_PROVIDER, DBNotPresentSimulatorConnectionProvider.class )
							.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
							.applySetting( AvailableSettings.TOLERATE_DATABASE_NOT_PRESENT, "true" );
				},
				metadataSources -> metadataSources.addAnnotatedClass( DCPTEntity.class ),
				Metadata::buildSessionFactory,
				DelayerConnectionProviderTest::checkDelayedInitializationIsCorrect
		);
	}

	private static void checkDelayedInitializationIsCorrect(SessionFactory sf, ServiceRegistry sr) {
		boolean exceptionRaised = false;
		try ( Session s = sf.openSession() ) {
			Transaction tx = s.beginTransaction();
			s.persist( new DCPTEntity() );
			s.flush();
			tx.commit();
		}
		catch (PersistenceException e) {
			//expected exception as the database is not present
			exceptionRaised = true;
		}
		finally {
			assertTrue( "A JDBC exception should have been raised as the DB is not accessible yet", exceptionRaised );
		}
		ConnectionProvider service = sr.getService( ConnectionProvider.class );
		if ( service.isUnwrappableAs( DBNotPresentSimulatorConnectionProvider.class  ) ) {
			DBNotPresentSimulatorConnectionProvider simulator = service.unwrap( DBNotPresentSimulatorConnectionProvider.class );
			// database is present now
			simulator.delay = false;

		}
		else {
			assertTrue("Test fails to configure itself with a DBNotPresentSimulatorConnectionProvider", false);
		}
		try ( Session s = sf.openSession() ) {
			Transaction tx = s.beginTransaction();
			DCPTEntity myEntity = new DCPTEntity();
			s.persist( myEntity );
			s.flush();
			s.delete( myEntity );
			tx.commit();
		}
	}

	private void doTestSessionFactoryCreation(
			Consumer<StandardServiceRegistryBuilder> configureSettings,
			Consumer<MetadataSources> configureEntities,
			Function<Metadata, SessionFactory> buildSessionFactory,
			BiConsumer<SessionFactory, ServiceRegistry> useSessionFactory) {
		ServiceRegistryImplementor serviceRegistry = null;
		try {
			StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
			configureSettings.accept( standardServiceRegistryBuilder );
			serviceRegistry	= (ServiceRegistryImplementor) standardServiceRegistryBuilder.build();
			MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			configureEntities.accept( metadataSources );

			Metadata metadata = metadataSources.buildMetadata();
			//need consumer and producer
			try (SessionFactory sessionFactory = buildSessionFactory.apply( metadata )) {
				useSessionFactory.accept( sessionFactory, serviceRegistry );
			}
		}
		finally {
			if ( serviceRegistry != null ) {
				try {
					StandardServiceRegistryBuilder.destroy( serviceRegistry );
				}
				catch (Exception ignore) {
				}
			}
		}
	}


	/**
	 * Throw exceptions during #getConnection() calls when the delay flag is set to true.
	 * The connection provider starts with the delay flag set to true.
	 *
	 * This is used ot simulate a database that is not ready.
	 */
	public static class DBNotPresentSimulatorConnectionProvider extends DriverManagerConnectionProviderImpl {
		public volatile boolean delay = true;

		public DBNotPresentSimulatorConnectionProvider() {
		}

		@Override
		public Connection getConnection() throws SQLException {
			if ( delay ) {
				throw new SQLException( "Database not present" );
			}
			return super.getConnection();
		}

		@Override
		public boolean isUnwrappableAs(Class unwrapType) {
			return DBNotPresentSimulatorConnectionProvider.class.equals( unwrapType ) || super.isUnwrappableAs( unwrapType );
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			if ( DBNotPresentSimulatorConnectionProvider.class.equals( unwrapType ) ) {
				return (T) this;
			}
			return super.unwrap( unwrapType );
		}
	}

	@Entity
	static class DCPTEntity {
		@Id @GeneratedValue
		public Integer id;
	}
}
