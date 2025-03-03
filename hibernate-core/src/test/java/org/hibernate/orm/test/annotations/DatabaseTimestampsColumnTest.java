/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;
import java.util.Date;
import java.util.EnumSet;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = DatabaseTimestampsColumnTest.Person.class)
public class DatabaseTimestampsColumnTest {

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId(mutable = true)
		private String name;

		@Column(nullable = false)
		@Timestamp
		private Date creationDate;

		@Column(nullable = true)
		@Timestamp(EventType.UPDATE)
		private Date editionDate;

		@Column(nullable = false, name="version")
		@Timestamp({ EventType.INSERT, EventType.UPDATE })
		private Date timestamp;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public Date getEditionDate() {
			return editionDate;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	}

	@ValueGenerationType(generatedBy = TimestampValueGeneration.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Timestamp { EventType[] value() default EventType.INSERT; }

	public static class TimestampValueGeneration implements OnExecutionGenerator {
		private EnumSet<EventType> events;

		public TimestampValueGeneration(Timestamp annotation, Member member, GeneratorCreationContext context) {
			events = EventTypeSets.fromArray( annotation.value() );
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return events;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { dialect.currentTimestamp() };
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}
	}

	@Test
	public void generatesCurrentTimestamp(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					Person person = new Person();
					person.setName("John Doe");
					entityManager.persist(person);
					entityManager.getTransaction().commit();
					Date creationDate = person.getCreationDate();
					Assertions.assertNotNull(creationDate);
					Assertions.assertNull(person.getEditionDate());
					Date timestamp = person.getTimestamp();
					Assertions.assertNotNull(timestamp);

					try { Thread.sleep(1_000); } catch (InterruptedException ie) {};

					entityManager.getTransaction().begin();
					person.setName("Jane Doe");
					entityManager.getTransaction().commit();
					Assertions.assertNotNull(person.getCreationDate());
					Assertions.assertEquals(creationDate, person.getCreationDate());
					Assertions.assertNotNull(person.getEditionDate());
					Assertions.assertNotNull(person.getTimestamp());
					Assertions.assertNotEquals(timestamp, person.getTimestamp());
				}
		);
	}
}
