/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = {OracleEnumTest.Timeslot.class, OracleEnumTest.Activity.class, OracleEnumTest.Weather.class, OracleEnumTest.Sky.class})
@RequiresDialect(value = OracleDialect.class, majorVersion = 23)
public class OracleEnumTest {

	@Test public void testNamedEnum(SessionFactoryScope scope) {
		Timeslot timeslot = new Timeslot();
		Activity activity = new Activity();
		activity.activityType = ActivityType.Play;
		timeslot.activity = activity;
		scope.inTransaction( s -> s.persist( timeslot ) );
		Timeslot ts = scope.fromTransaction( s-> s.createQuery("from Timeslot where activity.activityType = Play", Timeslot.class ).getSingleResult() );
		assertEquals( ts.activity.activityType, ActivityType.Play );
	}

	@Test public void testOrdinalEnum(SessionFactoryScope scope) {
		Weather weather = new Weather();
		Sky sky = new Sky();
		sky.skyType = SkyType.Sunny;
		weather.sky = sky;
		scope.inTransaction( s -> s.persist( weather ) );
		Weather w = scope.fromTransaction( s-> s.createQuery("from Weather where sky.skyType = Sunny", Weather.class ).getSingleResult() );
		assertEquals( w.sky.skyType, SkyType.Sunny );
	}

	@Test public void testSchema(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.doWork(
					c -> {
						try(Statement stmt = c.createStatement()) {
							try(ResultSet typeInfo = stmt.executeQuery("select name, decode(instr(data_display,'WHEN '''),0,'NUMBER','VARCHAR2') from user_domains where type='ENUMERATED'")) {
								while (typeInfo.next()) {
									String name = typeInfo.getString(1);
									String baseType = typeInfo.getString(2);
									if (name.equalsIgnoreCase("ActivityType") && baseType.equals("VARCHAR2")) {
										return;
									}
								}
							}
						}
						fail("named enum type not exported");
					}
			);
		});
		scope.inSession( s -> {
			s.doWork(
					c -> {
						ResultSet tableInfo = c.getMetaData().getColumns(null, null, "ACTIVITY", "ACTIVITYTYPE" );
						while ( tableInfo.next() ) {
							String type = tableInfo.getString(6);
							assertEquals( "VARCHAR2", type );
							return;
						}
						fail("named enum column not exported");
					}
			);
		});
	}

	public enum ActivityType {Work, Play, Sleep }

	@Entity(name = "Activity")
	public static class Activity {

		@Id
		@JdbcTypeCode(SqlTypes.NAMED_ENUM)
		ActivityType activityType;

	}

	@Entity(name = "Timeslot")
	public static class Timeslot {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "id")
		private int id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		Activity activity;
	}

	public enum SkyType {Sunny, Cloudy}
	@Entity(name = "Sky")
	public static class Sky {

		@Id
		@JdbcTypeCode(SqlTypes.NAMED_ORDINAL_ENUM)
		SkyType skyType;
	}

	@Entity(name = "Weather")
	public static class Weather {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "id")
		private int id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		Sky sky;
	}
}
