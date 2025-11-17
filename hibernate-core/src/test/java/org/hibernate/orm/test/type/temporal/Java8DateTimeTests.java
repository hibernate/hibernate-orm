/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.descriptor.java.JavaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Java8DateTimeTests.TheEntity.class)
@SessionFactory
public class Java8DateTimeTests {
	@Test
	public void test(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		var entityBinding = modelScope.getDomainModel().getEntityBinding( TheEntity.class.getName() );
		for ( Property propertyBinding : entityBinding.getPropertyClosure() ) {
			Assertions.assertFalse( propertyBinding.getType() instanceof SerializableType,
					"Found property bound as Serializable : " + propertyBinding.getName() );
		}

		factoryScope.inTransaction( (session) -> {
			var theEntity = new TheEntity( 1 );
			session.persist( theEntity );
		} );

		factoryScope.inTransaction( (session) -> {
			var theEntity = session.find( TheEntity.class, 1 );
			dump( entityBinding, theEntity );
			assertNotNull( theEntity );
			session.remove( theEntity );
		} );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void dump(PersistentClass entityBinding, TheEntity theEntity) {
		for ( Property propertyBinding : entityBinding.getPropertyClosure() ) {
			final JavaType javaType = ( (AbstractStandardBasicType) propertyBinding.getType() ).getJavaTypeDescriptor();
			System.out.printf(
					"%s (%s) -> %s%n",
					propertyBinding.getName(),
					javaType.getJavaTypeClass().getSimpleName(),
					javaType.toString(propertyBinding.getGetter(TheEntity.class).get(theEntity))
			);
		}
	}

	@Entity(name = "TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {

		@Id
		private Integer id;

		@Column(name = "local_date_time")
		private LocalDateTime localDateTime = LocalDateTime.now();

		@Column(name = "local_date")
		private LocalDate localDate = LocalDate.now();

		@Column(name = "local_time")
		private LocalTime localTime = LocalTime.now();

		@Column(name = "instant_value")
		private Instant instant = Instant.now();

		@Column(name = "zoned_date_time")
		private ZonedDateTime zonedDateTime = ZonedDateTime.now();

		@Column(name = "offset_date_time")
		private OffsetDateTime offsetDateTime = OffsetDateTime.now();

		@Column(name = "offset_time")
		private OffsetTime offsetTime = OffsetTime.now();

		@Column(name = "duration_value")
		private Duration duration = Duration.of( 20, ChronoUnit.DAYS );

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public void setLocalDateTime(LocalDateTime localDateTime) {
			this.localDateTime = localDateTime;
		}

		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public LocalTime getLocalTime() {
			return localTime;
		}

		public void setLocalTime(LocalTime localTime) {
			this.localTime = localTime;
		}

		public Instant getInstant() {
			return instant;
		}

		public void setInstant(Instant instant) {
			this.instant = instant;
		}

		public ZonedDateTime getZonedDateTime() {
			return zonedDateTime;
		}

		public void setZonedDateTime(ZonedDateTime zonedDateTime) {
			this.zonedDateTime = zonedDateTime;
		}

		public OffsetDateTime getOffsetDateTime() {
			return offsetDateTime;
		}

		public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
			this.offsetDateTime = offsetDateTime;
		}

		public OffsetTime getOffsetTime() {
			return offsetTime;
		}

		public void setOffsetTime(OffsetTime offsetTime) {
			this.offsetTime = offsetTime;
		}

		public Duration getDuration() {
			return duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}
	}
}
