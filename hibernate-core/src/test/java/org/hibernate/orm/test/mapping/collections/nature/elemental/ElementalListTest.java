/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.nature.elemental;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = ElementalListTest.Person.class )
@SessionFactory
public class ElementalListTest {
	private static final Logger log = Logger.getLogger( ElementalListTest.class );

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Person person = new Person( 1 );
			person.phones.add( "027-123-4567" );
			person.phones.add( "028-234-9876" );
			session.persist( person );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLifecycle(SessionFactoryScope scope) {
		scope.inTransaction( (entityManager) -> {
			Person person = entityManager.find( Person.class, 1 );
			log.info( "Clear element collection and add element" );
			//tag::ex-collection-elemental-lifecycle[]
			person.getPhones().clear();
			person.getPhones().add( "123-456-7890" );
			person.getPhones().add( "456-000-1234" );
			//end::ex-collection-elemental-lifecycle[]
		} );
		scope.inTransaction( (entityManager) -> {
			Person person = entityManager.find( Person.class, 1 );
			log.info( "Remove one element" );
			//tag::ex-collection-elemental-remove[]
			person.getPhones().remove( 0 );
			//end::ex-collection-elemental-remove[]
		} );
	}

	//tag::ex-collection-elemental-model[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		@ElementCollection
		@OrderColumn( name = "phone_position")
		private List<String> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::ex-collection-elemental-model[]


		private Person() {
			// used by Hibernate
		}

		public Person(Integer id) {
			this.id = id;
			this.phones = new ArrayList<>();
		}

		public List<String> getPhones() {
			return phones;
		}
	//tag::ex-collection-elemental-model[]
	}
	//end::ex-collection-elemental-model[]
}
