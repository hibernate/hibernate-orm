/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.ISOLATION;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12749")
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(
		settings = @Setting( name = ISOLATION, value = "REPEATABLE_READ" ),
		settingProviders = @SettingProvider( settingName = CONNECTION_PROVIDER, provider = C3P0DifferentIsolationLevelTest.ConnectionProviderProvider.class )
)
@DomainModel(annotatedClasses = C3P0DifferentIsolationLevelTest.Person.class)
@SessionFactory(useCollectingStatementInspector = true)
public class C3P0DifferentIsolationLevelTest {

	private static final C3P0ProxyConnectionProvider connectionProvider = new C3P0ProxyConnectionProvider();

	@Test
	public void testStoredProcedureOutParameter(SessionFactoryScope factoryScope) throws SQLException {
		var sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();
		connectionProvider.clear();

		factoryScope.inTransaction( (session) -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "Vlad Mihalcea";

			session.persist( person );
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ).toLowerCase() ).startsWith( "insert into " );
		Connection connectionSpy = connectionProvider.getConnectionSpyMap().keySet().iterator().next();
		verify( connectionSpy, times(1) ).setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );

		sqlCollector.clear();
		connectionProvider.clear();

		factoryScope.inTransaction( (session) -> {
			Person person = session.find( Person.class, 1L );

			assertThat( person.name ).isEqualTo( "Vlad Mihalcea" );
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ).toLowerCase() ).startsWith( "select " );
		connectionSpy = connectionProvider.getConnectionSpyMap().keySet().iterator().next();
		verify( connectionSpy, times(1) ).setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;
	}

	public static class ConnectionProviderProvider implements SettingProvider.Provider<C3P0ProxyConnectionProvider> {
		@Override
		public C3P0ProxyConnectionProvider getSetting() {
			return connectionProvider;
		}
	}
}
