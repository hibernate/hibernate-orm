/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
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
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "dd/MM/yyyy 'at' HH:mm:ssxxx" );

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new TimeZoneStorageEntity( 1, OFFSET_DATE_TIME, ZONED_DATE_TIME ) ) );
	}

	@AfterEach
	public void destroy(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.createMutationQuery( "delete from java.lang.Object" ).executeUpdate() );
	}

	@Test
	public void testOffsetRetainedAuto(SessionFactoryScope scope) {
		testOffsetRetained( scope, "Auto" );
	}

	@Test
	public void testOffsetRetainedColumn(SessionFactoryScope scope) {
		testOffsetRetained( scope, "Column" );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	public void testOffsetRetainedFormatAuto(SessionFactoryScope scope) {
		testOffsetRetainedFormat( scope, "Auto" );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	public void testOffsetRetainedFormatColumn(SessionFactoryScope scope) {
		testOffsetRetainedFormat( scope, "Column" );
	}

	public void testOffsetRetained(SessionFactoryScope scope, String suffix) {
		scope.inSession(
				session -> {
					List<Tuple> resultList = session.createQuery(
							"select " +
									"e.offsetDateTime" + suffix + ", " +
									"e.zonedDateTime" + suffix + ", " +
									"extract(offset from e.offsetDateTime" + suffix + "), " +
									"extract(offset from e.zonedDateTime" + suffix + "), " +
									"e.offsetDateTime" + suffix + " + 1 hour, " +
									"e.zonedDateTime" + suffix + " + 1 hour, " +
									"e.offsetDateTime" + suffix + " + 1 hour - e.offsetDateTime" + suffix + ", " +
									"e.zonedDateTime" + suffix + " + 1 hour - e.zonedDateTime" + suffix + ", " +
									"1 from TimeZoneStorageEntity e " +
									"where e.offsetDateTime" + suffix + " = e.offsetDateTime" + suffix,
							Tuple.class
					).getResultList();
					assertThat( resultList.get( 0 ).get( 0, OffsetDateTime.class ), Matchers.is( OFFSET_DATE_TIME ) );
					assertThat( resultList.get( 0 ).get( 1, ZonedDateTime.class ), Matchers.is( ZONED_DATE_TIME ) );
					assertThat( resultList.get( 0 ).get( 2, ZoneOffset.class ), Matchers.is( OFFSET_DATE_TIME.getOffset() ) );
					assertThat( resultList.get( 0 ).get( 3, ZoneOffset.class ), Matchers.is( ZONED_DATE_TIME.getOffset() ) );
					assertThat( resultList.get( 0 ).get( 4, OffsetDateTime.class ), Matchers.is( OFFSET_DATE_TIME.plusHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 5, ZonedDateTime.class ), Matchers.is( ZONED_DATE_TIME.plusHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 6, Duration.class ), Matchers.is( Duration.ofHours( 1L ) ) );
					assertThat( resultList.get( 0 ).get( 7, Duration.class ), Matchers.is( Duration.ofHours( 1L ) ) );
				}
		);
	}

	public void testOffsetRetainedFormat(SessionFactoryScope scope, String suffix) {
		scope.inSession(
				session -> {
					List<Tuple> resultList = session.createQuery(
							"select " +
									"format(e.offsetDateTime" + suffix + " as 'dd/MM/yyyy ''at'' HH:mm:ssxxx'), " +
									"format(e.zonedDateTime" + suffix + " as 'dd/MM/yyyy ''at'' HH:mm:ssxxx'), " +
									"1 from TimeZoneStorageEntity e " +
									"where e.offsetDateTime" + suffix + " = e.offsetDateTime" + suffix,
							Tuple.class
					).getResultList();
					assertThat( resultList.get( 0 ).get( 0, String.class ), Matchers.is( FORMATTER.format( OFFSET_DATE_TIME ) ) );
					assertThat( resultList.get( 0 ).get( 1, String.class ), Matchers.is( FORMATTER.format( ZONED_DATE_TIME ) ) );
				}
		);
	}

//	@Test
//	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
//	public void testNormalize(SessionFactoryScope scope) {
//		scope.inSession(
//				session -> {
//					List<Tuple> resultList = session.createQuery(
//							"select e.offsetDateTimeNormalized, extract(offset from e.offsetDateTimeNormalized), e.zonedDateTimeNormalized, extract(offset from e.zonedDateTimeNormalized) from TimeZoneStorageEntity e",
//							Tuple.class
//					).getResultList();
//					assertThat( resultList.get( 0 ).get( 0, OffsetDateTime.class ), Matchers.is( OFFSET_DATE_TIME ) );
//					assertThat( resultList.get( 0 ).get( 1, ZoneOffset.class ), Matchers.is( OFFSET_DATE_TIME.getOffset() ) );
//					assertThat( resultList.get( 0 ).get( 2, ZonedDateTime.class ), Matchers.is( ZONED_DATE_TIME ) );
//					assertThat( resultList.get( 0 ).get( 3, ZoneOffset.class ), Matchers.is( ZONED_DATE_TIME.getOffset() ) );
//				}
//		);
//	}

	@Entity(name = "TimeZoneStorageEntity")
	@Table(name = "TimeZoneStorageEntity")
	public static class TimeZoneStorageEntity {
		@Id
		private Integer id;

		//end::time-zone-column-examples-mapping-example[]
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
		@Column(name = "birthday_offset_auto")
		private OffsetDateTime offsetDateTimeAuto;

		@TimeZoneStorage
		@Column(name = "birthday_zoned_auto")
		private ZonedDateTime zonedDateTimeAuto;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
		@Column(name = "birthday_offset_normalized")
		private OffsetDateTime offsetDateTimeNormalized;

		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
		@Column(name = "birthday_zoned_normalized")
		private ZonedDateTime zonedDateTimeNormalized;

		public TimeZoneStorageEntity() {
		}

		public TimeZoneStorageEntity(Integer id, OffsetDateTime offsetDateTime, ZonedDateTime zonedDateTime) {
			this.id = id;
			this.offsetDateTimeColumn = offsetDateTime;
			this.zonedDateTimeColumn = zonedDateTime;
			this.offsetDateTimeAuto = offsetDateTime;
			this.zonedDateTimeAuto = zonedDateTime;
			this.offsetDateTimeNormalized = offsetDateTime;
			this.zonedDateTimeNormalized = zonedDateTime;
		}
	}
}
