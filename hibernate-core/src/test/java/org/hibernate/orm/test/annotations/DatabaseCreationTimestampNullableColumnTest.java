/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.EnumSet;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.OnExecutionGenerator;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( "HHH-11096" )
@RequiresDialectFeature( feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class )
@Jpa(
		annotatedClasses = {
				DatabaseCreationTimestampNullableColumnTest.Person.class
		}
)
public class DatabaseCreationTimestampNullableColumnTest {

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		@Column(nullable = false)
		@FunctionCreationTimestamp
		private Date creationDate;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

	}

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

	@Test
	public void generatesCurrentTimestamp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person = new Person();
					person.setName("John Doe");
					entityManager.persist(person);

					entityManager.flush();
					Assertions.assertNotNull(person.getCreationDate());
				}
		);
	}
}
