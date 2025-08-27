/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

@CompilationTest
public class ForcedAccessTypeTest {

	@Test
	@WithClasses({MyEntity.class, DateAndTime.class, MySuperclass.class})
	public void testCorrectAccessTypeUsedForEmbeddable() {
		System.out.println( TestUtil.getMetaModelSourceAsString( MyEntity.class ) );
		assertMetamodelClassGeneratedFor( MyEntity.class );

		assertMetamodelClassGeneratedFor( MySuperclass.class );
		assertAttributeTypeInMetaModelFor(
				MySuperclass.class,
				"name",
				String.class,
				"Missing attribute name of type java.lang.String"
		);

		assertMetamodelClassGeneratedFor( DateAndTime.class );
		assertAttributeTypeInMetaModelFor(
				DateAndTime.class,
				"localDateTime",
				LocalDateTime.class,
				"Missing attribute localDateTime of type java.time.LocalDateTime"
		);
		assertAttributeTypeInMetaModelFor(
				DateAndTime.class,
				"offset",
				ZoneOffset.class,
				"Missing attribute offset of type java.time.ZoneOffset"
		);
	}

	@Entity
	@Access(AccessType.FIELD)
	static class MyEntity extends MySuperclass {
		@Id
		Long id;

		@Embedded
		DateAndTime dateAndTime;
	}

	@MappedSuperclass
	@Access(AccessType.PROPERTY)
	static class MySuperclass {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	static class DateAndTime {

		@Transient
		private OffsetDateTime dateTime;

		public DateAndTime() {
			this.dateTime = OffsetDateTime.now();
		}

		public DateAndTime(final OffsetDateTime dateTime) {
			this.dateTime = dateTime;
		}

		public DateAndTime(final Instant localDateTime, final ZoneOffset offset) {
			this.dateTime = localDateTime.atOffset( offset );
		}

		public LocalDateTime getLocalDateTime() {
			return dateTime.toLocalDateTime();
		}

		public void setLocalDateTime(final LocalDateTime localDateTime) {
			this.dateTime = localDateTime.atOffset( this.dateTime.getOffset() );
		}

		public ZoneOffset getOffset() {
			return dateTime.getOffset();
		}

		public void setOffset(final ZoneOffset offset) {
			this.dateTime = dateTime.toLocalDateTime().atOffset( offset );
		}

		@Transient
		public OffsetDateTime dateTime() {
			return dateTime;
		}
	}

}
