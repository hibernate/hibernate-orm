/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = TimeZoneStorageMappingTests.TimeZoneStorageEntity.class)
@SessionFactory
@ServiceRegistry(settings = @Setting( name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "AUTO"))
public class TimeZoneStorageMappingTests {

	private static final ZoneOffset JVM_TIMEZONE_OFFSET = OffsetDateTime.now().getOffset();
	private static final OffsetTime OFFSET_TIME = OffsetTime.of(
			LocalTime.of(
					12,
					0,
					0
			),
			ZoneOffset.ofHoursMinutes( 5, 45 )
	);
	private static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.of(
			LocalDateTime.of(
					2022,
					3,
					1,
					12,
					0,
					0
			),
			ZoneOffset.ofHoursMinutes( 5, 45 )
	);
	private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(
			LocalDateTime.of(
					2022,
					3,
					1,
					12,
					0,
					0
			),
			ZoneOffset.ofHoursMinutes( 5, 45 )
	);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ssxxx" );
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "dd/MM/yyyy 'at' HH:mm:ssxxx" );

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new TimeZoneStorageEntity( 1, OFFSET_TIME, OFFSET_DATE_TIME, ZONED_DATE_TIME ) ) );
	}

	@AfterEach
	public void destroy(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	public void testOffsetRetainedAuto(SessionFactoryScope scope) {
		testOffsetRetained( scope, "Auto" );
	}

	@Test
	public void testOffsetRetainedColumn(SessionFactoryScope scope) {
		testOffsetRetained( scope, "Column" );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "Altibase doesn't allow function in date format string")
	public void testOffsetRetainedFormatAuto(SessionFactoryScope scope) {
		testOffsetRetainedFormat( scope, "Auto" );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "Altibase doesn't allow function in date format string")
	public void testOffsetRetainedFormatColumn(SessionFactoryScope scope) {
		testOffsetRetainedFormat( scope, "Column" );
	}

	public void testOffsetRetained(SessionFactoryScope scope, String suffix) {
		scope.inSession(
				session -> {
					List<Tuple> resultList = session.createQuery(
							"select " +
									"e.offsetTime" + suffix + ", " +
									"e.offsetDateTime" + suffix + ", " +
									"e.zonedDateTime" + suffix + ", " +
									"extract(offset from e.offsetTime" + suffix + "), " +
									"extract(offset from e.offsetDateTime" + suffix + "), " +
									"extract(offset from e.zonedDateTime" + suffix + "), " +
									"e.offsetTime" + suffix + " + 1 hour, " +
									"e.offsetDateTime" + suffix + " + 1 hour, " +
									"e.zonedDateTime" + suffix + " + 1 hour, " +
									"e.offsetTime" + suffix + " + 1 hour - e.offsetTime" + suffix + ", " +
									"e.offsetDateTime" + suffix + " + 1 hour - e.offsetDateTime" + suffix + ", " +
									"e.zonedDateTime" + suffix + " + 1 hour - e.zonedDateTime" + suffix + ", " +
									"1 from TimeZoneStorageEntity e " +
									"where e.offsetDateTime" + suffix + " = e.offsetDateTime" + suffix,
							Tuple.class
					).getResultList();
					assertThat( resultList.get( 0 ).get( 0, OffsetTime.class ), Matchers.is( OFFSET_TIME ) );
					assertThat( resultList.get( 0 ).get( 1, OffsetDateTime.class ), Matchers.is( OFFSET_DATE_TIME ) );
					assertThat( resultList.get( 0 ).get( 2, ZonedDateTime.class ), Matchers.is( ZONED_DATE_TIME ) );
					if ( !( scope.getSessionFactory().getJdbcServices().getDialect() instanceof H2Dialect) ) {
						// H2 bug: https://github.com/h2database/h2database/issues/3757
						assertThat(
								resultList.get( 0 ).get( 3, ZoneOffset.class ),
								Matchers.is( OFFSET_TIME.getOffset() )
						);
					}
					assertThat( resultList.get( 0 ).get( 4, ZoneOffset.class ), Matchers.is( OFFSET_DATE_TIME.getOffset() ) );
					assertThat( resultList.get( 0 ).get( 5, ZoneOffset.class ), Matchers.is( ZONED_DATE_TIME.getOffset() ) );
					assertThat( resultList.get( 0 ).get( 6, OffsetTime.class ), Matchers.is( OFFSET_TIME.plusHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 7, OffsetDateTime.class ), Matchers.is( OFFSET_DATE_TIME.plusHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 8, ZonedDateTime.class ), Matchers.is( ZONED_DATE_TIME.plusHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 9, Duration.class ), Matchers.is( Duration.ofHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 10, Duration.class ), Matchers.is( Duration.ofHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 11, Duration.class ), Matchers.is( Duration.ofHours( 1L ) ) );
				}
		);
	}

	public void testOffsetRetainedFormat(SessionFactoryScope scope, String suffix) {
		scope.inSession(
				session -> {
					List<Tuple> resultList = session.createQuery(
							"select " +
									"format(e.offsetTime" + suffix + " as 'HH:mm:ssxxx'), " +
									"format(e.offsetDateTime" + suffix + " as 'dd/MM/yyyy ''at'' HH:mm:ssxxx'), " +
									"format(e.zonedDateTime" + suffix + " as 'dd/MM/yyyy ''at'' HH:mm:ssxxx'), " +
									"1 from TimeZoneStorageEntity e " +
									"where e.offsetDateTime" + suffix + " = e.offsetDateTime" + suffix,
							Tuple.class
					).getResultList();
					if ( !( scope.getSessionFactory().getJdbcServices().getDialect() instanceof H2Dialect) ) {
						// H2 bug: https://github.com/h2database/h2database/issues/3757
						assertThat(
								resultList.get( 0 ).get( 0, String.class ),
								Matchers.is( TIME_FORMATTER.format( OFFSET_TIME ) )
						);
					}
					assertThat( resultList.get( 0 ).get( 1, String.class ), Matchers.is( FORMATTER.format( OFFSET_DATE_TIME ) ) );
					assertThat( resultList.get( 0 ).get( 2, String.class ), Matchers.is( FORMATTER.format( ZONED_DATE_TIME ) ) );
				}
		);
	}

	@Test
	public void testNormalize(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<Tuple> resultList = session.createQuery(
							"select " +
									"e.offsetTimeNormalized, " +
									"e.offsetDateTimeNormalized, " +
									"e.zonedDateTimeNormalized, " +
									"e.offsetTimeNormalizedUtc, " +
									"e.offsetDateTimeNormalizedUtc, " +
									"e.zonedDateTimeNormalizedUtc " +
									"from TimeZoneStorageEntity e",
							Tuple.class
					).getResultList();
					assertThat( resultList.get( 0 ).get( 0, OffsetTime.class ).toLocalTime(), Matchers.is( OFFSET_TIME.withOffsetSameInstant( JVM_TIMEZONE_OFFSET ).toLocalTime() ) );
					assertThat( resultList.get( 0 ).get( 0, OffsetTime.class ).getOffset(), Matchers.is( JVM_TIMEZONE_OFFSET ) );
					assertThat( resultList.get( 0 ).get( 1, OffsetDateTime.class ).toInstant(), Matchers.is( OFFSET_DATE_TIME.toInstant() ) );
					assertThat( resultList.get( 0 ).get( 2, ZonedDateTime.class ).toInstant(), Matchers.is( ZONED_DATE_TIME.toInstant() ) );
					assertThat( resultList.get( 0 ).get( 3, OffsetTime.class ).toLocalTime(), Matchers.is( OFFSET_TIME.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime() ) );
					assertThat( resultList.get( 0 ).get( 3, OffsetTime.class ).getOffset(), Matchers.is( ZoneOffset.UTC ) );
					assertThat( resultList.get( 0 ).get( 4, OffsetDateTime.class ).toInstant(), Matchers.is( OFFSET_DATE_TIME.toInstant() ) );
					assertThat( resultList.get( 0 ).get( 5, ZonedDateTime.class ).toInstant(), Matchers.is( ZONED_DATE_TIME.toInstant() ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimezoneTypes.class, comment = "Extracting the offset usually only makes sense if the temporal retains the offset. On DBs that have native TZ support we test this anyway to make sure it's not broken'")
	public void testNormalizeOffset(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<Tuple> resultList = session.createQuery(
							"select " +
									"extract(offset from e.offsetTimeNormalizedUtc), " +
									"extract(offset from e.offsetDateTimeNormalizedUtc), " +
									"extract(offset from e.zonedDateTimeNormalizedUtc) " +
									"from TimeZoneStorageEntity e",
							Tuple.class
					).getResultList();
					if ( !( scope.getSessionFactory().getJdbcServices().getDialect() instanceof H2Dialect) ) {
						// H2 bug: https://github.com/h2database/h2database/issues/3757
						assertThat( resultList.get( 0 ).get( 0, ZoneOffset.class ), Matchers.is( ZoneOffset.UTC ) );
					}
					assertThat( resultList.get( 0 ).get( 1, ZoneOffset.class ), Matchers.is( ZoneOffset.UTC ) );
					assertThat( resultList.get( 0 ).get( 2, ZoneOffset.class ), Matchers.is( ZoneOffset.UTC ) );
				}
		);
	}

	@Entity(name = "TimeZoneStorageEntity")
	@Table(name = "TimeZoneStorageEntity")
	public static class TimeZoneStorageEntity {
		@Id
		private Integer id;

		//tag::time-zone-column-examples-mapping-example[]
		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "birthtime_offset_offset")
		@Column(name = "birthtime_offset")
		private OffsetTime offsetTimeColumn;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "birthday_offset_offset")
		@Column(name = "birthday_offset")
		private OffsetDateTime offsetDateTimeColumn;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "birthday_zoned_offset")
		@Column(name = "birthday_zoned")
		private ZonedDateTime zonedDateTimeColumn;
		//end::time-zone-column-examples-mapping-example[]

		@TimeZoneStorage
		@Column(name = "birthtime_offset_auto")
		private OffsetTime offsetTimeAuto;

		@TimeZoneStorage
		@Column(name = "birthday_offset_auto")
		private OffsetDateTime offsetDateTimeAuto;

		@TimeZoneStorage
		@Column(name = "birthday_zoned_auto")
		private ZonedDateTime zonedDateTimeAuto;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
		@Column(name = "birthtime_offset_normalized")
		private OffsetTime offsetTimeNormalized;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
		@Column(name = "birthday_offset_normalized")
		private OffsetDateTime offsetDateTimeNormalized;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
		@Column(name = "birthday_zoned_normalized")
		private ZonedDateTime zonedDateTimeNormalized;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
		@Column(name = "birthtime_offset_utc")
		private OffsetTime offsetTimeNormalizedUtc;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
		@Column(name = "birthday_offset_utc")
		private OffsetDateTime offsetDateTimeNormalizedUtc;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
		@Column(name = "birthday_zoned_utc")
		private ZonedDateTime zonedDateTimeNormalizedUtc;

		public TimeZoneStorageEntity() {
		}

		public TimeZoneStorageEntity(Integer id, OffsetTime offsetTime, OffsetDateTime offsetDateTime, ZonedDateTime zonedDateTime) {
			this.id = id;
			this.offsetTimeColumn = offsetTime;
			this.offsetDateTimeColumn = offsetDateTime;
			this.zonedDateTimeColumn = zonedDateTime;
			this.offsetTimeAuto = offsetTime;
			this.offsetDateTimeAuto = offsetDateTime;
			this.zonedDateTimeAuto = zonedDateTime;
			this.offsetTimeNormalized = offsetTime;
			this.offsetDateTimeNormalized = offsetDateTime;
			this.zonedDateTimeNormalized = zonedDateTime;
			this.offsetTimeNormalizedUtc = offsetTime;
			this.offsetDateTimeNormalizedUtc = offsetDateTime;
			this.zonedDateTimeNormalizedUtc = zonedDateTime;
		}
	}
}
