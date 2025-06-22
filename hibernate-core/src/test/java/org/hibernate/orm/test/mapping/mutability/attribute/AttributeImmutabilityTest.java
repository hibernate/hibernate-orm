/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import java.util.Date;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

public class AttributeImmutabilityTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Event1.class, Event2.class
		};
	}

	@Test
	public void test1() {
		//tag::entity-immutability-persist-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event1 event = new Event1();
			event.setId(1L);
			event.setCreatedOn(new Date());
			event.setMessage("Hibernate User Guide rocks!");

			entityManager.persist(event);
		});
		//end::entity-immutability-persist-example[]
		//tag::entity-immutability-update-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event1 event = entityManager.find(Event1.class, 1L);
			log.info("Change event message");
			event.setMessage("Hibernate User Guide");
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event1 event = entityManager.find(Event1.class, 1L);
			assertEquals("Hibernate User Guide rocks!", event.getMessage());
		});
	}

	@Test
	public void test2() {
		//tag::entity-immutability-persist-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event2 event = new Event2();
			event.setId(1L);
			event.setCreatedOn(new Date());
			event.setMessage("Hibernate User Guide rocks!");

			entityManager.persist(event);
		});
		//end::entity-immutability-persist-example[]
		//tag::entity-immutability-update-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event2 event = entityManager.find(Event2.class, 1L);
			log.info("Change event message");
			event.setMessage("Hibernate User Guide");
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
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
