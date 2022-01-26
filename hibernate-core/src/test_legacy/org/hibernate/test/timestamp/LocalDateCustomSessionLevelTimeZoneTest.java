/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.timestamp;

import java.time.LocalDate;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = MySQLDialect.class)
public class LocalDateCustomSessionLevelTimeZoneTest extends BaseSessionFactoryFunctionalTest {

	private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(
			"Europe/Berlin" );

	private final ConnectionProviderDelegate connectionProvider = new ConnectionProviderDelegate() {
		@Override
		public void configure(Map<String, Object> configurationValues) {
			String url = (String) configurationValues.get( AvailableSettings.URL );
			if ( !url.contains( "?" ) ) {
				url += "?";
			}
			else if ( !url.endsWith( "&" ) ) {
				url += "&";
			}

			url += "useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin";

			configurationValues.put( AvailableSettings.URL, url );
			super.configure( configurationValues );
		}
	};

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER ) );
		builder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@AfterAll
	protected void releaseResources() {
		connectionProvider.stop();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11396")
	public void testTimeZone() {
		TimeZone old = TimeZone.getDefault();
		try {
			// The producer (MySQL) Berlin and returns 1980-01-01
			TimeZone jdbcTimeZone = TimeZone.getTimeZone( "Europe/Berlin" );
			TimeZone.setDefault( jdbcTimeZone );

			//hibernate.connection.url jdbc:mysql://localhost/hibernate_orm_test
			doInHibernateSessionBuilder( () -> sessionFactory().withOptions().jdbcTimeZone( TIME_ZONE ), s -> {
				Person person = new Person();
				person.id = 1L;
				s.persist( person );
			} );

			doInHibernateSessionBuilder( () -> sessionFactory().withOptions().jdbcTimeZone( TIME_ZONE ), s -> {
				Person person = s.find( Person.class, 1L );
				assertEquals( LocalDate.of( 2017, 3, 7 ), person.createdOn );
			} );
		}
		finally {
			TimeZone.setDefault( old );
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private LocalDate createdOn = LocalDate.of( 2017, 3, 7 );
	}
}

