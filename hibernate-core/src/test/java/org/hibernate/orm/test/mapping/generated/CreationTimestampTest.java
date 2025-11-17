/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {CreationTimestampTest.Event.class})
public class CreationTimestampTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::mapping-generated-CreationTimestamp-persist-example[]
			Event dateEvent = new Event();
			entityManager.persist(dateEvent);
			//end::mapping-generated-CreationTimestamp-persist-example[]
		});
	}

	//tag::mapping-generated-provided-creation-ex1[]
	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`timestamp`")
		@CreationTimestamp
		private Date timestamp;

		//Constructors, getters, and setters are omitted for brevity
	//end::mapping-generated-provided-creation-ex1[]

		public Event() {}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	//tag::mapping-generated-provided-creation-ex1[]
	}
	//end::mapping-generated-provided-creation-ex1[]
}
