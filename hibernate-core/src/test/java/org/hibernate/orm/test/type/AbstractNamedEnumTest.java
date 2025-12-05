/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Yanming Zhou
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel(annotatedClasses = {
		AbstractNamedEnumTest.Timeslot.class,
		AbstractNamedEnumTest.Activity.class,
		AbstractNamedEnumTest.Weather.class,
		AbstractNamedEnumTest.Sky.class
})
public abstract class AbstractNamedEnumTest {

	@Test public void testNamedEnum(SessionFactoryScope scope) {
		Timeslot timeslot = new Timeslot();
		Activity activity = new Activity();
		activity.activityType = ActivityType.Play;
		timeslot.activity = activity;
		scope.inTransaction( s -> s.persist( timeslot ) );
		Timeslot ts = scope.fromTransaction( s-> s.createQuery("from Timeslot where activity.activityType = Play", Timeslot.class ).getSingleResult() );
		assertEquals( ActivityType.Play, ts.activity.activityType );
	}

	@Test public void testNamedOrdinalEnum(SessionFactoryScope scope) {
		Weather weather = new Weather();
		Sky sky = new Sky();
		sky.skyType = SkyType.Sunny;
		weather.sky = sky;
		scope.inTransaction( s -> s.persist( weather ) );
		Weather w = scope.fromTransaction( s-> s.createQuery("from Weather where sky.skyType = Sunny", Weather.class ).getSingleResult() );
		assertEquals( SkyType.Sunny, w.sky.skyType );
	}

	@Test public void testSchema(SessionFactoryScope scope) {
		scope.inSession( s -> s.doWork( c -> {
			{
				var tableInfo = c.getMetaData().getColumns(
						null,
						null,
						normalizeNameForQueryingMetadata( "Activity" ),
						normalizeNameForQueryingMetadata( "ActivityType" )
				);
				assertTrue( tableInfo.next(), "named enum type not exported" );
				String type = tableInfo.getString( 6 );
				assertEquals( getDataTypeForNamedEnum( "ActivityType" ), type );
				assertFalse( tableInfo.next() );
			}
			{
				var tableInfo = c.getMetaData().getColumns(
						null,
						null,
						normalizeNameForQueryingMetadata( "Sky" ),
						normalizeNameForQueryingMetadata( "SkyType" )
				);
				assertTrue( tableInfo.next(), "named ordinal enum type not exported" );
				String type = tableInfo.getString( 6 );
				assertEquals( getDataTypeForNamedOrdinalEnum( "SkyType" ), type );
				assertFalse( tableInfo.next() );
			}
		} ) );
	}

	protected abstract String normalizeNameForQueryingMetadata(String name);

	protected abstract String getDataTypeForNamedEnum(String namedEnum);

	protected abstract String getDataTypeForNamedOrdinalEnum(String namedEnum);

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
