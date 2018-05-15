/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQL5Dialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernateSessionBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(MySQL5Dialect.class)
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class JdbcTimestampCustomSessionLevelTimeZoneTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( true, false );

	private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(
			"America/Los_Angeles" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void testTimeZone() {

		connectionProvider.clear();
		doInHibernateSessionBuilder( () -> {
			return sessionFactory().withOptions().jdbcTimeZone( TIME_ZONE );
		}, s -> {
			Person person = new Person();
			person.id = 1L;
			s.persist( person );

		} );

		assertEquals( 1, connectionProvider.getPreparedStatements().size() );
		PreparedStatement ps = connectionProvider.getPreparedStatements()
				.get( 0 );
		try {
			ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(
					Calendar.class );
			verify( ps, times( 1 ) ).setTimestamp(
					anyInt(),
					any( Timestamp.class ),
					calendarArgumentCaptor.capture()
			);
			assertEquals(
					TIME_ZONE,
					calendarArgumentCaptor.getValue().getTimeZone()
			);
		}
		catch ( SQLException e ) {
			fail( e.getMessage() );
		}

		connectionProvider.clear();
		doInHibernateSessionBuilder( () -> {
			return sessionFactory().withOptions().jdbcTimeZone( TIME_ZONE );
		}, s -> {
			s.doWork( connection -> {
				try ( Statement st = connection.createStatement() ) {
					try ( ResultSet rs = st.executeQuery(
							"select createdOn from Person" ) ) {
						while ( rs.next() ) {
							Timestamp timestamp = rs.getTimestamp( 1 );
							int offsetDiff = TimeZone.getDefault()
									.getOffset( 0 ) - TIME_ZONE.getOffset( 0 );
							assertEquals(
									Math.abs( Long.valueOf( offsetDiff )
													  .longValue() ),
									Math.abs( timestamp.getTime() )
							);
						}
					}
				}
			} );
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

