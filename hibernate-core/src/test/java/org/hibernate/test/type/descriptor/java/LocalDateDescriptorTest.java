/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.LocalDate;
import java.util.TimeZone;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;

import org.hibernate.testing.TestForIssue;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Gigov
 * @author Christian Beikov
 */
public class LocalDateDescriptorTest extends AbstractDescriptorTest<LocalDate> {
	final LocalDate original = LocalDate.of( 2016, 10, 8 );
	final LocalDate copy = LocalDate.of( 2016, 10, 8 );
	final LocalDate different = LocalDate.of( 2013, 8, 8 );

	public LocalDateDescriptorTest() {
		super( LocalDateJavaDescriptor.INSTANCE );
	}

	@Override
	protected Data<LocalDate> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11396")
	public void testTimeZoneHandling() {
		TimeZone old = TimeZone.getDefault();
		try {
			// The producer (MySQL) Berlin and returns 1980-01-01
			TimeZone jdbcTimeZone = TimeZone.getTimeZone( "Europe/Berlin" );
			TimeZone.setDefault( jdbcTimeZone );
			java.sql.Date date = java.sql.Date.valueOf( "1980-01-01" );

			// The consumer is in GMT, but the JDBC time zone is still Europe/Berlin
			TimeZone.setDefault( TimeZone.getTimeZone( "GMT" ) );
			LocalDate localDate = LocalDateJavaDescriptor.INSTANCE.wrap( date, new WrapperOptions() {
				public boolean useStreamForLobBinding() {
					return false;
				}

				public LobCreator getLobCreator() {
					return NonContextualLobCreator.INSTANCE;
				}

				public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
					return sqlTypeDescriptor;
				}

				@Override
				public TimeZone getJdbcTimeZone() {
					return jdbcTimeZone;
				}
			} );

			assertEquals( 1980, localDate.getYear() );
			assertEquals( 1, localDate.getMonthValue() );
			assertEquals( 1, localDate.getDayOfMonth() );
		}
		finally {
			TimeZone.setDefault( old );
		}
	}

}
