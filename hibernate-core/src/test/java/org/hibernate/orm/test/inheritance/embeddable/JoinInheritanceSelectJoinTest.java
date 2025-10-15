/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				JoinInheritanceSelectJoinTest.Human.class,
				JoinInheritanceSelectJoinTest.Parent.class,
				JoinInheritanceSelectJoinTest.Child.class,
		}
)
@SessionFactory
@JiraKey("HHH-19607")
class JoinInheritanceSelectJoinTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( "Luigi", new Address( "Via Milano", "Roma" ) );
					Child child = new Child( "Miriam" );
					child.addParent( parent );
					session.persist( child );
				}
		);
	}

	@AfterEach
	public void teardown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@FailureExpected(jiraKey = "HHH-19607")
	void testSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Child> children = session
					.createSelectionQuery(
							"SELECT c FROM Child c LEFT JOIN c.parents p WHERE p.address is not null",
							Child.class
					)
					.getResultList();

			assertThat( children.size() ).isEqualTo( 1 );
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-19607")
	void testSelectWithParameters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Child> children = session
					.createSelectionQuery(
							"SELECT c FROM Child c LEFT JOIN c.parents p WHERE p.address = :address",
							Child.class
					)
					.setParameter( "address", new Address( "Via Milano", "Roma" )  )
					.getResultList();

			assertThat( children.size() ).isEqualTo( 1 );
		} );
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToMany(cascade = CascadeType.PERSIST)
		private Set<Parent> parents = new HashSet<>();

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Parent> getParents() {
			return parents;
		}

		public void addParent(Parent parent) {
			this.parents.add( parent );
		}
	}

	@Entity(name = "Human")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Human {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private Address address;

		public Human() {
		}

		public Human(String name, Address address) {
			this.name = name;
			this.address = address;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Address getAddress() {
			return address;
		}
	}

	@Entity(name = "Parent")
	public static class Parent extends Human {
		public Parent() {
		}

		public Parent(String name, Address address) {
			super( name, address );
		}
	}

	@Embeddable
	public static class Address {
		private String street;

		private String city;

		public Address() {
		}

		public Address(String street, String city) {
			this.street = street;
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Address address = (Address) o;
			return Objects.equals( street, address.street ) && Objects.equals( city, address.city );
		}

		@Override
		public int hashCode() {
			return Objects.hash( street, city );
		}
	}

}
