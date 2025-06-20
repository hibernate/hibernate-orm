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


import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */

@RequiresDialectFeature( feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class )
@RequiresDialectFeature( feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class )
@DomainModel( annotatedClasses = ComplexValueGenerationTests.AuditedEntity.class )
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class ComplexValueGenerationTests {
	@Test
	public void testGenerations(SessionFactoryScope scope) {
		// first test creation
		final AuditedEntity saved = scope.fromTransaction( (session) -> {
			final AuditedEntity entity = new AuditedEntity( 1 );

			assertThat( entity.createdDate ).isNull();
			assertThat( entity.alwaysDate ).isNull();
			assertThat( entity.vmCreatedDate ).isNull();
			assertThat( entity.vmCreatedSqlDate ).isNull();
			assertThat( entity.vmCreatedSqlTime ).isNull();
			assertThat( entity.vmCreatedSqlTimestamp ).isNull();
			assertThat( entity.vmCreatedSqlLocalDate ).isNull();
			assertThat( entity.vmCreatedSqlLocalTime ).isNull();
			assertThat( entity.vmCreatedSqlLocalDateTime ).isNull();
			assertThat( entity.vmCreatedSqlMonthDay ).isNull();
			assertThat( entity.vmCreatedSqlOffsetDateTime ).isNull();
			assertThat( entity.vmCreatedSqlOffsetTime ).isNull();
			assertThat( entity.vmCreatedSqlYear ).isNull();
			assertThat( entity.vmCreatedSqlYearMonth ).isNull();
			assertThat( entity.vmCreatedSqlZonedDateTime ).isNull();

			session.persist( entity );

			return entity;
		} );

		assertThat( saved ).isNotNull();
		assertThat( saved.createdDate ).isNotNull();
		assertThat( saved.alwaysDate ).isNotNull();
		assertThat( saved.vmCreatedDate ).isNotNull();
		assertThat( saved.vmCreatedSqlDate ).isNotNull();
		assertThat( saved.vmCreatedSqlTime ).isNotNull();
		assertThat( saved.vmCreatedSqlTimestamp ).isNotNull();
		assertThat( saved.vmCreatedSqlLocalDate ).isNotNull();
		assertThat( saved.vmCreatedSqlLocalTime ).isNotNull();
		assertThat( saved.vmCreatedSqlLocalDateTime ).isNotNull();
		assertThat( saved.vmCreatedSqlMonthDay ).isNotNull();
		assertThat( saved.vmCreatedSqlOffsetDateTime ).isNotNull();
		assertThat( saved.vmCreatedSqlOffsetTime ).isNotNull();
		assertThat( saved.vmCreatedSqlYear ).isNotNull();
		assertThat( saved.vmCreatedSqlYearMonth ).isNotNull();
		assertThat( saved.vmCreatedSqlZonedDateTime ).isNotNull();

		// next, try mutating
		saved.lastName = "changed";
		final AuditedEntity merged = scope.fromSession( (session) -> {
			// next, try mutating
			return (AuditedEntity) session.merge( saved );
		} );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdDate ).isNotNull();
		assertThat( merged.alwaysDate ).isNotNull();
		assertThat( merged.vmCreatedDate ).isNotNull();
		assertThat( merged.vmCreatedSqlDate ).isNotNull();
		assertThat( merged.vmCreatedSqlTime ).isNotNull();
		assertThat( merged.vmCreatedSqlTimestamp ).isNotNull();
		assertThat( merged.vmCreatedSqlLocalDate ).isNotNull();
		assertThat( merged.vmCreatedSqlLocalTime ).isNotNull();
		assertThat( merged.vmCreatedSqlLocalDateTime ).isNotNull();
		assertThat( merged.vmCreatedSqlMonthDay ).isNotNull();
		assertThat( merged.vmCreatedSqlOffsetDateTime ).isNotNull();
		assertThat( merged.vmCreatedSqlOffsetTime ).isNotNull();
		assertThat( merged.vmCreatedSqlYear ).isNotNull();
		assertThat( merged.vmCreatedSqlYearMonth ).isNotNull();
		assertThat( merged.vmCreatedSqlZonedDateTime ).isNotNull();

		// lastly, make sure we can load it..
		scope.inTransaction( (session) -> {
			assertThat( session.get( AuditedEntity.class, 1 ) ).isNotNull();
		} );
	}


	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "AuditedEntity" )
	@Table( name = "ann_generated_complex" )
	public static class AuditedEntity {
		@Id
		private Integer id;

		@Generated
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Date createdDate;

		@Generated(event = { EventType.INSERT, EventType.UPDATE } )
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

		private AuditedEntity() {
		}

		private AuditedEntity(Integer id) {
			this.id = id;
		}
	}
}
