/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.entity;

import java.util.Date;

import org.hibernate.annotations.Immutable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@DomainModel( annotatedClasses = EntityImmutabilityTest.Event.class )
@SessionFactory
public class EntityImmutabilityTest {
	private static final Logger log = Logger.getLogger( EntityImmutabilityTest.class );

	@Test
	void verifyMetamodel(DomainModelScope scope) {
		scope.withHierarchy( Event.class, (entity) -> {
			assertThat( entity.isMutable() ).isFalse();

			// this implies that all attributes and mapped columns are non-updateable,
			// but the code does not explicitly set that.  The functional test
			// verifies that they function as non-updateable
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( (entityManager) -> {
			//tag::entity-immutability-persist-example[]
			Event event = new Event();
			event.setId(1L);
			event.setCreatedOn(new Date());
			event.setMessage("Hibernate User Guide rocks!");

			entityManager.persist(event);
			//end::entity-immutability-persist-example[]
		} );

		scope.inTransaction( (entityManager) -> {
			//tag::entity-immutability-update-example[]
			Event event = entityManager.find(Event.class, 1L);
			log.info("Change event message");
			event.setMessage("Hibernate User Guide");
			//end::entity-immutability-update-example[]
		} );
		scope.inTransaction( (entityManager) -> {
			Event event = entityManager.find(Event.class, 1L);
			assertThat( event.getMessage() ).isEqualTo( "Hibernate User Guide rocks!" );
		} );
	}

	//tag::entity-immutability-example[]
	@Entity(name = "Event")
	@Immutable
	public static class Event {

		@Id
		private Long id;

		private Date createdOn;

		private String message;

		//Getters and setters are omitted for brevity

	//end::entity-immutability-example[]

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
		//tag::entity-immutability-example[]
	}
	//end::entity-immutability-example[]
}
