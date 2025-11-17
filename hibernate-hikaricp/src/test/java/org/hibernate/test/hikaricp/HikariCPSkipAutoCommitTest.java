/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.util.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "The jTDS driver doesn't implement Connection#isValid so this fails")
@ServiceRegistry(
		settings = {
				@Setting( name = CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, value = "true" ),
				@Setting( name = "hibernate.hikari.autoCommit", value = "false" )
		},
		settingProviders = @SettingProvider( settingName = CONNECTION_PROVIDER, provider = HikariCPSkipAutoCommitTest.ConnectionProviderProvider.class)
)
@DomainModel( annotatedClasses = HikariCPSkipAutoCommitTest.City.class )
@SessionFactory
public class HikariCPSkipAutoCommitTest {
	private static final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@AfterAll
	static void afterAll() {
		connectionProvider.stop();
	}

	@Test
	public void testIt(SessionFactoryScope factoryScope) throws Exception {
		// force creation of the SF
		factoryScope.getSessionFactory();

		connectionProvider.clear();

		factoryScope.inTransaction( session -> {
			City city = new City();
			city.setId( 1L );
			city.setName( "Cluj-Napoca" );
			session.persist( city );

			assertThat( connectionProvider.getAcquiredConnections() ).isEmpty();
			assertThat( connectionProvider.getReleasedConnections() ).isEmpty();
		} );
		verifyConnections();

		connectionProvider.clear();
		factoryScope.inTransaction( session -> {
			City city = session.find( City.class, 1L );
			assertThat(  city.getName() ).isEqualTo( "Cluj-Napoca" );
		} );
		verifyConnections();
	}

	private void verifyConnections() {
		assertThat( connectionProvider.getAcquiredConnections() ).isEmpty();

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertThat( connections ).hasSize( 1 );
		try {
			List<Object[]> setAutoCommitCalls = connectionProvider.spyContext.getCalls(
					Connection.class.getMethod( "setAutoCommit", boolean.class ),
					connections.get( 0 )
			);
			assertThat( setAutoCommitCalls ).as( "setAutoCommit should never be called" ).isEmpty();
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
