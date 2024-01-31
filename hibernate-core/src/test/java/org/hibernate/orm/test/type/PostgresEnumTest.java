package org.hibernate.orm.test.type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = {PostgresEnumTest.Timeslot.class, PostgresEnumTest.Activity.class})
@RequiresDialect(PostgreSQLDialect.class)
public class PostgresEnumTest {

	@Test public void test(SessionFactoryScope scope) {
		Timeslot timeslot = new Timeslot();
		Activity activity = new Activity();
		activity.activityType = ActivityType.Play;
		timeslot.activity = activity;
		scope.inTransaction( s -> s.persist( timeslot ) );
		Timeslot ts = scope.fromTransaction( s-> s.createQuery("from Timeslot where activity.activityType = Play", Timeslot.class ).getSingleResult() );
		assertEquals( ts.activity.activityType, ActivityType.Play );
	}

	@Test public void testSchema(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.doWork(
					c -> {
						ResultSet typeInfo = c.getMetaData().getTypeInfo();
						while ( typeInfo.next() ) {
							String name = typeInfo.getString(1);
							if ( name.equalsIgnoreCase("ActivityType") ) {
								return;
							}
						}
						fail("named enum type not exported");
					}
			);
		});
		scope.inSession( s -> {
			s.doWork(
					c -> {
						ResultSet tableInfo = c.getMetaData().getColumns(null, null, "activity", "activitytype" );
						while ( tableInfo.next() ) {
							String type = tableInfo.getString(6);
							assertEquals( "activitytype", type );
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
}
