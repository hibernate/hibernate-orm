/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
public class JdbcTimestampDefaultTimeZoneTest
		extends BaseSessionFactoryFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
			true,
			false
	);

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
	public void testTimeZone() {

		connectionProvider.clear();
		inTransaction( s -> {
			Person person = new Person();
			person.id = 1L;
			s.persist( person );

		} );

		assertEquals( 1, connectionProvider.getPreparedStatements().size() );
		PreparedStatement ps = connectionProvider.getPreparedStatements()
				.get( 0 );
		try {
			verify( ps, times( 1 ) ).setTimestamp(
					anyInt(),
					any( Timestamp.class )
			);
		}
		catch (SQLException e) {
			fail( e.getMessage() );
		}

		inTransaction( s -> {
			Person person = s.find( Person.class, 1L );
			assertEquals( 0, person.createdOn.getTime() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Timestamp createdOn = new Timestamp( 0 );
	}
}

