/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = InMemoryTimestampGenerationTest.Person.class,
		settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME,
				provider = MutableClockSettingProvider.class))
@Jira("https://hibernate.atlassian.net/browse/HHH-19840")
public class InMemoryTimestampGenerationTest {
	private MutableClock clock;

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		clock = CurrentTimestampGeneration.getClock( scope.getEntityManagerFactory().unwrap( SessionFactory.class ) );
		clock.reset();
	}

	@Test
	public void test(EntityManagerFactoryScope scope) throws InterruptedException {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setFirstName( "Jon" );
			person.setLastName( "Doe" );
			entityManager.persist( person );

			entityManager.flush();

			assertNotNull( person.getCreatedOn() );
			assertNotNull( person.getUpdatedOn() );
		} );

		clock.tick();
		sleep( 1 );

		scope.inTransaction( entityManager -> {
			final Person person = entityManager.find( Person.class, 1L );
			person.setLastName( "Doe Jr." );

			final var updatedOn = person.getUpdatedOn();
			final var createdOn = person.getCreatedOn();

			entityManager.flush();

			assertEquals( person.getCreatedOn(), createdOn );
			assertTrue( person.getUpdatedOn().isAfter( updatedOn ) );
		} );
	}

	@Entity(name = "Person")
	static class Person {
		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Column(nullable = false)
		@CreationTimestamp(source= SourceType.VM)
		private Instant createdOn;

		@Column(nullable = false)
		@UpdateTimestamp(source= SourceType.VM)
		private Instant updatedOn;

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

		public Instant getCreatedOn() {
			return createdOn;
		}

		public Instant getUpdatedOn() {
			return updatedOn;
		}
	}
}
