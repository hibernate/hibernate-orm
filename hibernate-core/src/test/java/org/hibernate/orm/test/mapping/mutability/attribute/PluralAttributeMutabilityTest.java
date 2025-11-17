/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.Immutable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Vlad Mihalcea
 */
@DomainModel( annotatedClasses = {
		PluralAttributeMutabilityTest.Batch.class,
		PluralAttributeMutabilityTest.Event.class
} )
@SessionFactory
public class PluralAttributeMutabilityTest {
	private static final Logger log = Logger.getLogger( PluralAttributeMutabilityTest.class );

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( (entityManager) -> {
			//tag::collection-immutability-persist-example[]
			Batch batch = new Batch();
			batch.setId(1L);
			batch.setName("Change request");

			Event event1 = new Event();
			event1.setId(1L);
			event1.setCreatedOn(new Date());
			event1.setMessage("Update Hibernate User Guide");

			Event event2 = new Event();
			event2.setId(2L);
			event2.setCreatedOn(new Date());
			event2.setMessage("Update Hibernate Getting Started Guide");

			batch.getEvents().add(event1);
			batch.getEvents().add(event2);

			entityManager.persist(batch);
			//end::collection-immutability-persist-example[]
		} );

		scope.inTransaction( (entityManager) -> {
			//tag::collection-entity-update-example[]
			Batch batch = entityManager.find(Batch.class, 1L);
			log.info("Change batch name");
			batch.setName("Proposed change request");
			//end::collection-entity-update-example[]
		} );

		//tag::collection-immutability-update-example[]
		try {
			//end::collection-immutability-update-example[]
			scope.inTransaction( (entityManager) -> {
				//tag::collection-immutability-update-example[]
				Batch batch = entityManager.find( Batch.class, 1L );
				batch.getEvents().clear();
				//end::collection-immutability-update-example[]
			} );
		//tag::collection-immutability-update-example[]
		}
		catch (Exception e) {
			log.error("Immutable collections cannot be modified");
		}
		//end::collection-immutability-update-example[]
	}

	//tag::collection-immutability-example[]
	@Entity(name = "Batch")
	public static class Batch {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@Immutable
		private List<Event> events = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::collection-immutability-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Event> getEvents() {
			return events;
		}
		//tag::collection-immutability-example[]
	}

	@Entity(name = "Event")
	@Immutable
	public static class Event {

		@Id
		private Long id;

		private Date createdOn;

		private String message;

		//Getters and setters are omitted for brevity

	//end::collection-immutability-example[]

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
		//tag::collection-immutability-example[]
	}
	//end::collection-immutability-example[]
}
