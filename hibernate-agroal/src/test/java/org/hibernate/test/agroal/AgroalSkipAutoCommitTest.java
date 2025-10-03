/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.agroal;

import org.hibernate.test.agroal.util.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.AUTOCOMMIT;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class AgroalSkipAutoCommitTest {
	private static final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();
	private static final SettingProvider.Provider<PreparedStatementSpyConnectionProvider> connectionProviderProvider = () -> connectionProvider;

	@Test
	@ServiceRegistry(
			settings = {
					@Setting( name = CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, value = "true" ),
					@Setting( name = AUTOCOMMIT, value = "false" )
			},
			settingProviders = @SettingProvider(settingName = CONNECTION_PROVIDER, provider = ConnectionProviderProvider.class)
	)
	@DomainModel( annotatedClasses = City.class )
	@SessionFactory
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();
		connectionProvider.clear();
		factoryScope.inTransaction( (session) -> {
			City city = new City();
			city.setId( 1L );
			city.setName( "Cluj-Napoca" );
			session.persist( city );

			assertThat( connectionProvider.getAcquiredConnections().isEmpty() ).isTrue();
			assertThat( connectionProvider.getReleasedConnections().isEmpty() ).isTrue();
		} );
		verifyConnections();

		connectionProvider.clear();
		factoryScope.inTransaction(  (session) -> {
			City city = session.find( City.class, 1L );
			assertThat( city.getName() ).isEqualTo( "Cluj-Napoca" );
		} );
		verifyConnections();
	}

	private void verifyConnections() {
		assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertEquals( 1, connections.size() );
		try {
			List<Object[]> setAutoCommitCalls = connectionProvider.spyContext.getCalls(
					Connection.class.getMethod( "setAutoCommit", boolean.class ),
					connections.get( 0 )
			);
			assertTrue( "setAutoCommit should never be called", setAutoCommitCalls.isEmpty() );
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class ConnectionProviderProvider implements SettingProvider.Provider<PreparedStatementSpyConnectionProvider> {
		@Override
		public PreparedStatementSpyConnectionProvider getSetting() {
			return connectionProvider;
		}
	}
}
