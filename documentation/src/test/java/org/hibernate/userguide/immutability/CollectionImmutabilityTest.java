/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.immutability;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Immutable;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class CollectionImmutabilityTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( CollectionImmutabilityTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Batch.class,
			Event.class
		};
	}

	@Test
	public void test() {
		//tag::collection-immutability-persist-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			Batch batch = new Batch();
			batch.setId( 1L );
			batch.setName( "Change request" );

			Event event1 = new Event();
			event1.setId( 1L );
			event1.setCreatedOn( new Date( ) );
			event1.setMessage( "Update Hibernate User Guide" );

			Event event2 = new Event();
			event2.setId( 2L );
			event2.setCreatedOn( new Date( ) );
			event2.setMessage( "Update Hibernate Getting Started Guide" );

			batch.getEvents().add( event1 );
			batch.getEvents().add( event2 );

			entityManager.persist( batch );
		} );
		//end::collection-immutability-persist-example[]
		//tag::collection-entity-update-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			Batch batch = entityManager.find( Batch.class, 1L );
			log.info( "Change batch name" );
			batch.setName( "Proposed change request" );
		} );
		//end::collection-entity-update-example[]
		//tag::collection-immutability-update-example[]
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Batch batch = entityManager.find( Batch.class, 1L );
				batch.getEvents().clear();
			} );
		}
		catch ( Exception e ) {
			log.error( "Immutable collections cannot be modified" );
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
		private List<Event> events = new ArrayList<>( );

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
