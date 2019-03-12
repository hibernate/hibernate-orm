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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.junit.runners.Parameterized;

/**
 * Tests for storage of LocalDate properties.
 */
@TestForIssue(jiraKey = "HHH-10371")
public class LocalDateTest extends AbstractJavaTimeTypeTest<LocalDate, LocalDateTest.EntityWithLocalDate> {

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int year, int month, int day, ZoneId defaultTimeZone) {
			return add( defaultTimeZone, year, month, day );
		}
	}

	@Parameterized.Parameters(name = "{1}-{2}-{3} {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				// Not affected by HHH-13266 (JDK-8061577)
				.add( 2017, 11, 6, ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, ZONE_PARIS )
				.add( 1970, 1, 1, ZONE_GMT )
				.add( 1900, 1, 1, ZONE_GMT )
				.add( 1900, 1, 1, ZONE_OSLO )
				.add( 1900, 1, 2, ZONE_PARIS )
				.add( 1900, 1, 2, ZONE_AMSTERDAM )
				// Could have been affected by HHH-13266 (JDK-8061577), but was not
				.add( 1892, 1, 1, ZONE_OSLO )
				.add( 1900, 1, 1, ZONE_PARIS )
				.add( 1900, 1, 1, ZONE_AMSTERDAM )
				.add( 1600, 1, 1, ZONE_AMSTERDAM )
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
		statement.setDate( parameterIndex, getExpectedJdbcValueAfterHibernateWrite() );
	}

	@Override
	protected Date getExpectedJdbcValueAfterHibernateWrite() {
		return new Date( year - 1900, month - 1, day );
	}

	@Override
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		return resultSet.getDate( columnIndex );
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
}
