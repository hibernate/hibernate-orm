/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {AttributeImmutabilityTest.Event1.class, AttributeImmutabilityTest.Event2.class})
public class AttributeImmutabilityTest {

	@Test
	public void test1(EntityManagerFactoryScope scope) {
		//tag::entity-immutability-persist-example[]
		scope.inTransaction( entityManager -> {
			Event1 event = new Event1();
			event.setId(1L);
			event.setCreatedOn(new Date());
			event.setMessage("Hibernate User Guide rocks!");

			entityManager.persist(event);
		});
		//end::entity-immutability-persist-example[]
		//tag::entity-immutability-update-example[]
		scope.inTransaction( entityManager -> {
			Event1 event = entityManager.find(Event1.class, 1L);
			// "Change event message"
			event.setMessage("Hibernate User Guide");
		});
		scope.inTransaction( entityManager -> {
			Event1 event = entityManager.find(Event1.class, 1L);
			assertEquals("Hibernate User Guide rocks!", event.getMessage());
		});
	}

	@Test
	public void test2(EntityManagerFactoryScope scope) {
		//tag::entity-immutability-persist-example[]
		scope.inTransaction( entityManager -> {
			Event2 event = new Event2();
			event.setId(1L);
			event.setCreatedOn(new Date());
			event.setMessage("Hibernate User Guide rocks!");

			entityManager.persist(event);
		});
		//end::entity-immutability-persist-example[]
		//tag::entity-immutability-update-example[]
		scope.inTransaction( entityManager -> {
			Event2 event = entityManager.find(Event2.class, 1L);
			// "Change event message"
			event.setMessage("Hibernate User Guide");
		});
		scope.inTransaction( entityManager -> {
			Event2 event = entityManager.find(Event2.class, 1L);
			assertEquals("Hibernate User Guide rocks!", event.getMessage());
		});
	}


	@Entity(name = "Event1")
	public static class Event1 {

		@Id
		private Long id;

		@Immutable
		private Date createdOn;

		@Immutable
		private String message;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@Entity(name = "Event2")
	public static class Event2 {

		@Id
		private Long id;

		@Column(updatable = false)
		private Date createdOn;

		@Column(updatable = false)
		private String message;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
