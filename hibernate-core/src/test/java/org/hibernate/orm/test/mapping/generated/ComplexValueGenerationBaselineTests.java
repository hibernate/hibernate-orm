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
import java.util.GregorianCalendar;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Baseline testing of typically generated value types - mainly temporal types
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = ComplexValueGenerationBaselineTests.NonAuditedEntity.class )
@SessionFactory
public class ComplexValueGenerationBaselineTests {
	@Test
	public void testLoading(SessionFactoryScope scope) {
		// some of the generated-value tests show problems loading entities with attributes of
		// java.sql.Date type.  Make sure we can load such an entity without generation involved
		final NonAuditedEntity saved = scope.fromTransaction( (session) -> {
			final NonAuditedEntity entity = new NonAuditedEntity( 1 );
			session.persist( entity );
			return entity;
		} );

		// lastly, make sure we can load it..
		scope.inTransaction( (session) -> {
			assertThat( session.get( NonAuditedEntity.class, 1 ) ).isNotNull();
		} );
	}


	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "NonAuditedEntity" )
	@Table( name = "ann_generated_complex_base" )
	public static class NonAuditedEntity {
		@Id
		private Integer id;
		private String name;
		private String lastName;

		private Date createdDate;
		private Calendar alwaysDate;
		private Date vmCreatedDate;
		private Calendar vmCreatedCalendar;
		private java.sql.Date vmCreatedSqlDate;
		private Time vmCreatedSqlTime;
		private Timestamp vmCreatedSqlTimestamp;
		private Instant vmCreatedSqlInstant;
		private LocalDate vmCreatedSqlLocalDate;
		private LocalTime vmCreatedSqlLocalTime;
		private LocalDateTime vmCreatedSqlLocalDateTime;
		private MonthDay vmCreatedSqlMonthDay;
		private OffsetDateTime vmCreatedSqlOffsetDateTime;
		private OffsetTime vmCreatedSqlOffsetTime;
		private Year vmCreatedSqlYear;
		private YearMonth vmCreatedSqlYearMonth;
		private ZonedDateTime vmCreatedSqlZonedDateTime;
		private Timestamp updated;

		private NonAuditedEntity() {
		}

		private NonAuditedEntity(Integer id) {
			this.id = id;

			name = "it";

			createdDate = new Date();
			alwaysDate = new GregorianCalendar();
			vmCreatedDate = new Date();
			vmCreatedCalendar = new GregorianCalendar();
			vmCreatedSqlDate = new java.sql.Date( System.currentTimeMillis() );
			vmCreatedSqlTime = new Time( System.currentTimeMillis() );
			vmCreatedSqlTimestamp = new Timestamp( System.currentTimeMillis() );
			vmCreatedSqlInstant = Instant.now();
			vmCreatedSqlLocalDate = LocalDate.now();
			vmCreatedSqlLocalTime = LocalTime.now();
			vmCreatedSqlLocalDateTime = LocalDateTime.now();
			vmCreatedSqlMonthDay = MonthDay.now();
			vmCreatedSqlOffsetDateTime = OffsetDateTime.now();
			vmCreatedSqlOffsetTime = OffsetTime.now();
			vmCreatedSqlYear = Year.now();
			vmCreatedSqlYearMonth = YearMonth.now();
			vmCreatedSqlZonedDateTime = ZonedDateTime.now();
			updated = new Timestamp( System.currentTimeMillis() );
		}
	}
}
