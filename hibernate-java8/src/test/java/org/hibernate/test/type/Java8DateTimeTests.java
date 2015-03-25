/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.type;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class Java8DateTimeTests extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	public void basicTests() {
		final PersistentClass entityBinding = metadata().getEntityBinding( TheEntity.class.getName() );
		final Iterator propertyBindingIterator = entityBinding.getPropertyClosureIterator();
		while ( propertyBindingIterator.hasNext() ) {
			final Property propertyBinding = (Property) propertyBindingIterator.next();
			assertFalse(
					"Found property bound as Serializable : " + propertyBinding.getName(),
					propertyBinding.getType() instanceof SerializableType
			);
		}

		TheEntity theEntity = new TheEntity( 1 );

		Session s = openSession();
		s.beginTransaction();
		s.save( theEntity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		theEntity = (TheEntity) s.get( TheEntity.class, 1 );
		dump( entityBinding, theEntity );
		assertNotNull( theEntity );
		s.delete( theEntity );
		s.getTransaction().commit();
		s.close();
	}

	private void dump(PersistentClass entityBinding, TheEntity theEntity) {
		final Iterator propertyBindingIterator = entityBinding.getPropertyClosureIterator();
		while ( propertyBindingIterator.hasNext() ) {
			final Property propertyBinding = (Property) propertyBindingIterator.next();
			final JavaTypeDescriptor javaTypeDescriptor = ( (AbstractStandardBasicType) propertyBinding.getType() ).getJavaTypeDescriptor();

			System.out.println(
					String.format(
							"%s (%s) -> %s",
							propertyBinding.getName(),
							javaTypeDescriptor.getJavaTypeClass().getSimpleName(),
							javaTypeDescriptor.toString( propertyBinding.getGetter( TheEntity.class ).get( theEntity ) )
					)
			);
		}
	}

	@Entity(name = "TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		private Integer id;
		private LocalDateTime localDateTime = LocalDateTime.now();
		private LocalDate localDate = LocalDate.now();
		private LocalTime localTime = LocalTime.now();
		private Instant instant = Instant.now();
		private ZonedDateTime zonedDateTime = ZonedDateTime.now();
		private OffsetDateTime offsetDateTime = OffsetDateTime.now();
		private OffsetTime offsetTime = OffsetTime.now();
		private Duration duration = Duration.of( 20, ChronoUnit.DAYS );

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}

		@Id
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
