/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.reflect.Member;
import java.util.EnumSet;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class GeneratorTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		//tag::mapping-generated-GeneratorType-persist-example[]
		CurrentUser.INSTANCE.logIn("Alice");

		doInJPA(this::entityManagerFactory, entityManager -> {

			Person person = new Person();
			person.setId(1L);
			person.setFirstName("John");
			person.setLastName("Doe");

			entityManager.persist(person);
		});

		CurrentUser.INSTANCE.logOut();
		//end::mapping-generated-GeneratorType-persist-example[]

		//tag::mapping-generated-GeneratorType-update-example[]
		CurrentUser.INSTANCE.logIn("Bob");

		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.setFirstName("Mr. John");
		});

		CurrentUser.INSTANCE.logOut();
		//end::mapping-generated-GeneratorType-update-example[]
	}

	//tag::mapping-generated-GeneratorType-example[]
	public static class CurrentUser {

		public static final CurrentUser INSTANCE = new CurrentUser();

		private static final ThreadLocal<String> storage = new ThreadLocal<>();

		public void logIn(String user) {
			storage.set(user);
		}

		public void logOut() {
			storage.remove();
		}

		public String get() {
			return storage.get();
		}
	}

	@ValueGenerationType( generatedBy = LoggedUserGenerator.class)
	public @interface CurrentUserGeneration {
		EventType[] timing() default EventType.INSERT;
	}

	public static class LoggedUserGenerator implements BeforeExecutionGenerator {
		private final EnumSet<EventType> events;

		public LoggedUserGenerator(CurrentUserGeneration annotation, Member member, GeneratorCreationContext context) {
			this.events = EventTypeSets.fromArray( annotation.timing() );
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return CurrentUser.INSTANCE.get();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return events;
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@CurrentUserGeneration
		private String createdBy;

		@CurrentUserGeneration( timing = {EventType.INSERT, EventType.UPDATE} )
		private String updatedBy;

	//end::mapping-generated-GeneratorType-example[]
		public Person() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getCreatedBy() {
			return createdBy;
		}

		public String getUpdatedBy() {
			return updatedBy;
		}

		//tag::mapping-generated-GeneratorType-example[]
	}
	//end::mapping-generated-GeneratorType-example[]
}
