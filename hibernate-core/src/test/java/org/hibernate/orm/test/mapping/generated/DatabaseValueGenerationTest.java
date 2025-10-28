/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.EnumSet;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.OnExecutionGenerator;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {DatabaseValueGenerationTest.Event.class})
public class DatabaseValueGenerationTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Event dateEvent = new Event();
			entityManager.persist(dateEvent);
		});
	}

	//tag::mapping-database-generated-value-example[]
	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`timestamp`")
		@FunctionCreationTimestamp
		private Date timestamp;

		//Constructors, getters, and setters are omitted for brevity
	//end::mapping-database-generated-value-example[]

		public Event() {}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	//tag::mapping-database-generated-value-example[]
	}
	//end::mapping-database-generated-value-example[]

	//tag::mapping-database-generated-value-example[]

	@ValueGenerationType(generatedBy = FunctionCreationValueGeneration.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FunctionCreationTimestamp {}

	public static class FunctionCreationValueGeneration implements OnExecutionGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { dialect.currentTimestamp() };
		}
	}
	//end::mapping-database-generated-value-example[]
}
