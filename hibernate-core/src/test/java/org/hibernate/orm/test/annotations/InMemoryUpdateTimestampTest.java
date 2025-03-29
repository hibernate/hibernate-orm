/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey("HHH-13256")
public class InMemoryUpdateTimestampTest extends BaseEntityManagerFunctionalTestCase {

	private static final MutableClock clock = new MutableClock();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( CurrentTimestampGeneration.CLOCK_SETTING_NAME, clock );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setFirstName( "Jon" );
			person.setLastName( "Doe" );
			entityManager.persist( person );

			entityManager.flush();
			Assert.assertNotNull( person.getUpdatedOn() );
		} );
		clock.tick();

		AtomicReference<Date> beforeTimestamp = new AtomicReference<>();

		sleep( 1 );

		Person _person = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			beforeTimestamp.set( person.getUpdatedOn() );
			person.setLastName( "Doe Jr." );

			return person;
		} );

		assertTrue( _person.getUpdatedOn().after( beforeTimestamp.get() ) );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Column(nullable = false)
		@UpdateTimestamp
		private Date updatedOn;

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

		public Date getUpdatedOn() {
			return updatedOn;
		}
	}
}
