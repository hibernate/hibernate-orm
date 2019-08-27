/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.runners.Parameterized;

/**
 * Tests for storage of LocalDate properties.
 */
@TestForIssue(jiraKey = "HHH-10371")
@SkipForDialect(value = AbstractHANADialect.class,
		comment = "HANA systematically returns the wrong date when the JVM default timezone is not UTC")
@SkipForDialect(value = MySQL5Dialect.class,
		comment = "HHH-13582: MySQL ConnectorJ 8.x returns the wrong date"
				+ " when the JVM default timezone is different from the server timezone:"
				+ " https://bugs.mysql.com/bug.php?id=91112"
)
public class LocalDateTest extends AbstractJavaTimeTypeTest<LocalDate, LocalDateTest.EntityWithLocalDate> {

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int year, int month, int day, ZoneId defaultTimeZone) {
			return add( defaultTimeZone, year, month, day );
		}
	}

	@Parameterized.Parameters(name = "{1}-{2}-{3} {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				.alsoTestRemappingsWithH2( DateAsTimestampRemappingH2Dialect.class )
				// Not affected by HHH-13266 (JDK-8061577)
				.add( 2017, 11, 6, ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								.add( 1970, 1, 1, ZONE_GMT )
								.add( 1900, 1, 1, ZONE_GMT )
								.add( 1900, 1, 1, ZONE_OSLO )
								.add( 1900, 1, 2, ZONE_PARIS )
								.add( 1900, 1, 2, ZONE_AMSTERDAM )
								// Affected by HHH-13266 (JDK-8061577), but only when remapping dates as timestamps
								.add( 1892, 1, 1, ZONE_OSLO )
								.add( 1900, 1, 1, ZONE_PARIS )
								.add( 1900, 1, 1, ZONE_AMSTERDAM )
								.add( 1600, 1, 1, ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// It doesn't seem that any date at midnight can be affected by HHH-13379, but we add some tests just in case
				// => Test the day of DST end
				.add( 2018, 10, 28, ZONE_PARIS )
				.add( 2018, 9, 30, ZONE_AUCKLAND )
				.add( 2018, 5, 13, ZONE_SANTIAGO ) // DST end: 00:00 => 23:00 previous day
				// => Also test the day of DST start
				.add( 2018, 3, 25, ZONE_PARIS )
				.add( 2018, 9, 30, ZONE_AUCKLAND )
				.add( 2018, 8, 12, ZONE_SANTIAGO ) // DST start: 00:00 => 01:00
				.build();
	}

	private final int year;
	private final int month;
	private final int day;

	public LocalDateTest(EnvironmentParameters env, int year, int month, int day) {
		super( env );
		this.year = year;
		this.month = month;
		this.day = day;
	}

	@Override
	protected Class<EntityWithLocalDate> getEntityType() {
		return EntityWithLocalDate.class;
	}

	@Override
	protected EntityWithLocalDate createEntityForHibernateWrite(int id) {
		return new EntityWithLocalDate( id, getExpectedPropertyValueAfterHibernateRead() );
	}

	@Override
	protected LocalDate getExpectedPropertyValueAfterHibernateRead() {
		return LocalDate.of( year, month, day );
	}

	@Override
	protected LocalDate getActualPropertyValue(EntityWithLocalDate entity) {
		return entity.value;
	}

	@Override
	protected void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex) throws SQLException {
		if ( DateAsTimestampRemappingH2Dialect.class.equals( getRemappingDialectClass() ) ) {
			statement.setTimestamp( parameterIndex, new Timestamp( year - 1900, month - 1, day, 0, 0, 0, 0 ) );
		}
		else {
			statement.setDate( parameterIndex, new Date( year - 1900, month - 1, day ) );
		}
	}

	@Override
	protected Object getExpectedJdbcValueAfterHibernateWrite() {
		if ( DateAsTimestampRemappingH2Dialect.class.equals( getRemappingDialectClass() ) ) {
			return new Timestamp( year - 1900, month - 1, day, 0, 0, 0, 0 );
		}
		else {
			return new Date( year - 1900, month - 1, day );
		}
	}

	@Override
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		if ( DateAsTimestampRemappingH2Dialect.class.equals( getRemappingDialectClass() ) ) {
			return resultSet.getTimestamp( columnIndex );
		}
		else {
			return resultSet.getDate( columnIndex );
		}
	}

	@Entity(name = ENTITY_NAME)
	static final class EntityWithLocalDate {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = PROPERTY_COLUMN_NAME)
		private LocalDate value;

		protected EntityWithLocalDate() {
		}

		private EntityWithLocalDate(int id, LocalDate value) {
			this.id = id;
			this.value = value;
		}
	}

	public static class DateAsTimestampRemappingH2Dialect extends AbstractRemappingH2Dialect {
		public DateAsTimestampRemappingH2Dialect() {
			super( Types.DATE, TimestampTypeDescriptor.INSTANCE );
		}
	}
}
