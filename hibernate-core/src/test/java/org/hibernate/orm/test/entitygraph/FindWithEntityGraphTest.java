package org.hibernate.orm.test.entitygraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(
		annotatedClasses = {
				FindWithEntityGraphTest.Person.class,
				FindWithEntityGraphTest.PersonContact.class
		}
)
@JiraKey("HHH-16960")
public class FindWithEntityGraphTest {


	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person = new Person( 1L, "test" );

					Person child1 = new Person( 2L, "child1" );
					Person child2 = new Person( 3L, "child2" );
					child1.addParent( person );
					child2.addParent( person );
					entityManager.persist( person );
				}
		);
	}

	@Test
	public void testFind(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EntityGraph<?> personGraph = entityManager.createEntityGraph( Person.class );
					personGraph.addAttributeNodes( "children" );

					Person loadedPerson = entityManager.find(
							Person.class,
							1l,
							Map.of( "javax.persistence.fetchgraph", personGraph )
					);

					PersonContact personContact = loadedPerson.getPersonContact();
					assertNull( personContact );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Person parent;

		@OneToOne(mappedBy = "person", orphanRemoval = true, cascade = CascadeType.ALL)
		private PersonContact personContact;

		@OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = CascadeType.ALL)
		private Set<Person> children = new HashSet<>( 0 );

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

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

		public PersonContact getPersonContact() {
			return personContact;
		}

		public void setPersonContact(PersonContact personContact) {
			this.personContact = personContact;
		}

		public Person getParent() {
			return parent;
		}

		public void setParent(Person parent) {
			this.parent = parent;
		}

		public void addParent(Person parent) {
			this.parent = parent;
			parent.getChildren().add( this );
		}

		public Set<Person> getChildren() {
			return children;
		}

		public void setChildren(Set<Person> children) {
			this.children = children;
		}
	}

	@Entity(name = "PersonContact")
	public static class PersonContact {
		@Id
		private Long id;

		@Column
		private String address;

		public PersonContact() {
		}

		public PersonContact(String address) {
			this.address = address;
		}

		@OneToOne(optional = false, fetch = FetchType.LAZY)
		@MapsId
		private Person person;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}


}
