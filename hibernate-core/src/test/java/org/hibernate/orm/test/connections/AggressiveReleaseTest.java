/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.TransactionSettings.ENABLE_LAZY_LOAD_NO_TRANS;
import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Implementation of AggressiveReleaseTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0"),
				@Setting(name = TRANSACTION_COORDINATOR_STRATEGY, value = "jta"),
				@Setting(name = ENABLE_LAZY_LOAD_NO_TRANS, value = "true")
		},
		settingProviders = @SettingProvider(
				settingName = Environment.CONNECTION_HANDLING,
				provider = AggressiveReleaseTest.ConnectionmHandlingProvider.class
		),
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
public class AggressiveReleaseTest extends ConnectionManagementTestCase {

	public static class ConnectionmHandlingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.toString();
		}
	}

	@Override
	protected Session getSessionUnderTest(SessionFactoryScope scope) {
		return scope.getSessionFactory().openSession();
	}

	@Override
	protected void reconnect(Session session) {
	}

	@Override
	protected void prepare() throws Throwable {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
	}

	@Override
	protected void done() throws Throwable {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	// Some additional tests specifically for the aggressive-release functionality...

	@Test
	public void testSerializationOnAfterStatementAggressiveRelease(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session s = null;
		try {
			s = getSessionUnderTest( scope );
			Silly silly = new Silly( "silly" );
			s.persist( silly );

			// this should cause the CM to obtain a connection, and then release it
			s.flush();

			// We should be able to serialize the session at this point...
			SerializationHelper.serialize( s );

			s.remove( silly );
			s.flush();

		}
		finally {
			release( s, scope );
			done();
		}
	}

	@Test
	public void testSerializationFailsOnAfterStatementAggressiveReleaseWithOpenResources(SessionFactoryScope scope)
			throws Throwable {
		prepare();
		try (Session s = getSessionUnderTest( scope )) {

			Silly silly = new Silly( "silly" );
			s.persist( silly );

			// this should cause the CM to obtain a connection, and then release it
			s.flush();

			// both scroll() and iterate() cause batching to hold on
			// to resources, which should make aggressive-release not release
			// the connection (and thus cause serialization to fail)
			try (ScrollableResults<Silly> sr = s.createQuery( "from Silly", Silly.class ).scroll()) {
				sr.next();

				try {
					SerializationHelper.serialize( s );
					fail( "Serialization allowed on connected session; or aggressive release released connection with open resources" );
				}
				catch (IllegalStateException e) {
					// expected behavior
				}

				// getting the first row only because SybaseASE15Dialect throws NullPointerException
				// if data is not read before closing the ResultSet
				sr.next();

				// Closing the ScrollableResults does currently force batching to
				// aggressively release the connection
			}
			SerializationHelper.serialize( s );

			s.remove( silly );
			s.flush();
		}
		done();
	}

	@Test
	public void testQueryScrolling(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session s = null;
		try {
			s = getSessionUnderTest(scope);
			Silly silly = new Silly( "silly" );
			s.persist( silly );
			s.flush();

			try (ScrollableResults<Silly> sr = s.createQuery( "from Silly", Silly.class ).scroll()) {
				assertThat( sr.next() ).isTrue();
				Silly silly2 = sr.get();
				assertThat( silly2 ).isEqualTo( silly );
			}

			try (ScrollableResults<Silly> sr = s.createQuery( "from Silly", Silly.class ).scroll();
				ScrollableResults<Silly> sr2 = s.createQuery( "from Silly where name = 'silly'", Silly.class )
						.scroll()) {
				assertThat( sr.next() ).isTrue();
				assertThat( sr.get() ).isEqualTo( silly );
				assertThat( sr2.next() ).isTrue();
				assertThat( sr2.get() ).isEqualTo( silly );
			}

			s.remove( silly );
			s.flush();
		}
		finally {
			release( s, scope );
			done();
		}
	}

	@Test
	public void testSuppliedConnection(SessionFactoryScope scope) throws Throwable {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		prepare();

		Connection originalConnection = sessionFactory.getServiceRegistry().getService( ConnectionProvider.class )
				.getConnection();
		Session session = null;
		try {
			session = sessionFactory.withOptions().connection( originalConnection ).openSession();

			Silly silly = new Silly( "silly" );
			session.persist( silly );

			// this will cause the connection manager to cycle through the aggressive release logic;
			// it should not release the connection since we explicitly suplied it ourselves.
			session.flush();
			assertThat( session.isConnected() ).isTrue();

			session.remove( silly );
			session.flush();
		}
		finally {
			release( session, scope );
			done();
			sessionFactory.getServiceRegistry().getService( ConnectionProvider.class )
					.closeConnection( originalConnection );

		}
	}

	@Test
	public void testConnectionMaintanenceDuringFlush(SessionFactoryScope scope) throws Throwable {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		final Statistics statistics = sessionFactory.getStatistics();
		prepare();

		Session s = null;
		try {
			s = getSessionUnderTest( scope );

			List<Silly> entities = new ArrayList<>();
			for ( int i = 0; i < 10; i++ ) {
				Other other = new Other( "other-" + i );
				Silly silly = new Silly( "silly-" + i, other );
				entities.add( silly );
				s.persist( silly );
			}
			s.flush();

			for ( Silly silly : entities ) {
				silly.setName( "new-" + silly.getName() );
				silly.getOther().setName( "new-" + silly.getOther().getName() );
			}
			long initialCount = statistics.getConnectCount();
			s.flush();
			assertThat( statistics.getConnectCount() )
					.describedAs( "connection not maintained through flush" )
					.isEqualTo( initialCount + 1 );

			s.createMutationQuery( "delete from Silly" ).executeUpdate();
			s.createMutationQuery( "delete from Other" ).executeUpdate();
			s.getTransaction().commit();
		}
		finally {
			release( s, scope );
			done();
		}

	}
}
