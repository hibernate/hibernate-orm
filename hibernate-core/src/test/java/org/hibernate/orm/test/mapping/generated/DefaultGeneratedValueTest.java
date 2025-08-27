/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.orm.test.annotations.MutableClock;
import org.hibernate.orm.test.annotations.MutableClockSettingProvider;


import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the generation of column values using different value generation strategies.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class )
@RequiresDialectFeature( feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class )
@ServiceRegistry(settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME, provider = MutableClockSettingProvider.class))
@DomainModel( annotatedClasses = DefaultGeneratedValueTest.TheEntity.class )
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class DefaultGeneratedValueTest {

	private MutableClock clock;

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		clock = CurrentTimestampGeneration.getClock( scope.getSessionFactory() );
		clock.reset();
	}

	@Test
	@JiraKey( "HHH-2907" )
	public void testGeneration(SessionFactoryScope scope) {
		final TheEntity created = scope.fromTransaction( (s) -> {
			final TheEntity theEntity = new TheEntity( 1 );
			assertNull( theEntity.createdDate );
			assertNull( theEntity.alwaysDate );
			assertNull( theEntity.vmCreatedDate );
			assertNull( theEntity.vmCreatedSqlDate );
			assertNull( theEntity.vmCreatedSqlTime );
			assertNull( theEntity.vmCreatedSqlTimestamp );

			assertNull( theEntity.vmCreatedSqlLocalDate );
			assertNull( theEntity.vmCreatedSqlLocalTime );
			assertNull( theEntity.vmCreatedSqlLocalDateTime );
			assertNull( theEntity.vmCreatedSqlMonthDay );
			assertNull( theEntity.vmCreatedSqlOffsetDateTime );
			assertNull( theEntity.vmCreatedSqlOffsetTime );
			assertNull( theEntity.vmCreatedSqlYear );
			assertNull( theEntity.vmCreatedSqlYearMonth );
			assertNull( theEntity.vmCreatedSqlZonedDateTime );

			assertNull( theEntity.name );
			s.persist( theEntity );
			//TODO: Actually the values should be non-null after save
			assertNull( theEntity.createdDate );
			assertNull( theEntity.alwaysDate );
			assertNull( theEntity.vmCreatedDate );
			assertNull( theEntity.vmCreatedSqlDate );
			assertNull( theEntity.vmCreatedSqlTime );
			assertNull( theEntity.vmCreatedSqlTimestamp );
			assertNull( theEntity.vmCreatedSqlLocalDate );
			assertNull( theEntity.vmCreatedSqlLocalTime );
			assertNull( theEntity.vmCreatedSqlLocalDateTime );
			assertNull( theEntity.vmCreatedSqlMonthDay );
			assertNull( theEntity.vmCreatedSqlOffsetDateTime );
			assertNull( theEntity.vmCreatedSqlOffsetTime );
			assertNull( theEntity.vmCreatedSqlYear );
			assertNull( theEntity.vmCreatedSqlYearMonth );
			assertNull( theEntity.vmCreatedSqlZonedDateTime );
			assertNull( theEntity.name );

			return theEntity;
		} );

		assertNotNull( created.createdDate );
		assertNotNull( created.alwaysDate );
		assertEquals( "Bob", created.name );

		scope.inTransaction( (s) -> {
			final TheEntity theEntity = s.get( TheEntity.class, 1 );
			assertNotNull( theEntity.createdDate );
			assertNotNull( theEntity.alwaysDate );
			assertNotNull( theEntity.vmCreatedDate );
			assertNotNull( theEntity.vmCreatedSqlDate );
			assertNotNull( theEntity.vmCreatedSqlTime );
			assertNotNull( theEntity.vmCreatedSqlTimestamp );
			assertNotNull( theEntity.vmCreatedSqlLocalDate );
			assertNotNull( theEntity.vmCreatedSqlLocalTime );
			assertNotNull( theEntity.vmCreatedSqlLocalDateTime );
			assertNotNull( theEntity.vmCreatedSqlMonthDay );
			assertNotNull( theEntity.vmCreatedSqlOffsetDateTime );
			assertNotNull( theEntity.vmCreatedSqlOffsetTime );
			assertNotNull( theEntity.vmCreatedSqlYear );
			assertNotNull( theEntity.vmCreatedSqlYearMonth );
			assertNotNull( theEntity.vmCreatedSqlZonedDateTime );
			assertEquals( "Bob", theEntity.name );

			s.remove( theEntity );
		} );
	}

	@Test
	@JiraKey("HHH-2907")
	public void testUpdateTimestampGeneration(SessionFactoryScope scope) {
		final TheEntity created = scope.fromTransaction( (s) -> {
			TheEntity theEntity = new TheEntity( 1 );
			assertNull( theEntity.updated );
			s.persist( theEntity );
			assertNull( theEntity.updated );

			return theEntity;
		} );

		assertNotNull( created.vmCreatedSqlTimestamp );
		assertNotNull( created.updated );

		clock.tick();

		scope.inTransaction( (s) -> {
			final TheEntity theEntity = s.get( TheEntity.class, 1 );
			theEntity.lastName = "Smith";
		} );

		scope.inTransaction( (s) -> {
			final TheEntity theEntity = s.get( TheEntity.class, 1 );

			assertEquals( "Creation timestamp should not change on update", created.vmCreatedSqlTimestamp, theEntity.vmCreatedSqlTimestamp );
			assertTrue( "Update timestamp should have changed due to update", theEntity.updated.after( created.updated ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "TheEntity" )
	@Table( name = "T_ENT_GEN_DEF" )
	public static class TheEntity {
		@Id
		private Integer id;

		@Generated
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Date createdDate;

		@Generated( event = { EventType.INSERT, EventType.UPDATE } )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Calendar alwaysDate;

		@CreationTimestamp
		private Date vmCreatedDate;

		@CreationTimestamp
		private Calendar vmCreatedCalendar;

		@CreationTimestamp
		private java.sql.Date vmCreatedSqlDate;

		@CreationTimestamp
		private Time vmCreatedSqlTime;

		@CreationTimestamp
		private Timestamp vmCreatedSqlTimestamp;

		@CreationTimestamp
		private Instant vmCreatedSqlInstant;

		@CreationTimestamp
		private LocalDate vmCreatedSqlLocalDate;

		@CreationTimestamp
		private LocalTime vmCreatedSqlLocalTime;

		@CreationTimestamp
		private LocalDateTime vmCreatedSqlLocalDateTime;

		@CreationTimestamp
		private MonthDay vmCreatedSqlMonthDay;

		@CreationTimestamp
		private OffsetDateTime vmCreatedSqlOffsetDateTime;

		@CreationTimestamp
		private OffsetTime vmCreatedSqlOffsetTime;

		@CreationTimestamp
		private Year vmCreatedSqlYear;

		@CreationTimestamp
		private YearMonth vmCreatedSqlYearMonth;

		@CreationTimestamp
		private ZonedDateTime vmCreatedSqlZonedDateTime;

		@UpdateTimestamp
		private Timestamp updated;

		@StaticGeneration( value = "Bob" )
		private String name;

		@SuppressWarnings("unused")
		private String lastName;

		private TheEntity() {
		}

		private TheEntity(Integer id) {
			this.id = id;
		}
	}
}
