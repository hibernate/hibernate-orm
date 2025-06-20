/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Davide D'Alto
 */
@JiraKey(value = "HHH-12332")
@DomainModel(
		annotatedClasses = {
				TablePerClassInheritanceTest.Person.class,
				TablePerClassInheritanceTest.Child.class,
				TablePerClassInheritanceTest.Man.class,
				TablePerClassInheritanceTest.Woman.class
		}
)
@SessionFactory
public class TablePerClassInheritanceTest {
	private final Man john = new Man( "John", "Riding Roller Coasters" );
	private final Woman jane = new Woman( "Jane", "Hippotherapist" );
	private final Child susan = new Child( "Susan", "Super Mario retro Mushroom" );
	private final Child mark = new Child( "Mark", "Fidget Spinner" );
	private final List<Child> children = new ArrayList<>( Arrays.asList( susan, mark ) );
	private final List<Person> familyMembers = Arrays.asList( john, jane, susan, mark );

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			jane.setHusband( john );
			jane.setChildren( children );

			john.setWife( jane );
			john.setChildren( children );

			for ( Child child : children ) {
				child.setFather( john );
				child.setMother( jane );
			}

			for ( Person person : familyMembers ) {
				session.persist( person );
			}
		} );

	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectHierarchyRoot(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Person> people = session.createQuery( "FROM Person p", Person.class ).getResultList();
			assertThat( people.size(), is( familyMembers.size() ) );
			for ( Person person : people ) {
				if ( person instanceof Man ) {
					assertThat( ( (Man) person ).getHobby(), is( john.getHobby() ) );
				}
				else if ( person instanceof Woman ) {
					assertThat( ( (Woman) person ).getJob(), is( jane.getJob() ) );
				}
				else if ( person instanceof Child ) {
					if ( person.getName().equals( "Susan" ) ) {
						assertThat( ( (Child) person ).getFavouriteToy(), is( susan.getFavouriteToy() ) );
					}
					else {
						assertThat( ( (Child) person ).getFavouriteToy(), is( mark.getFavouriteToy() ) );
					}
				}
				else {
					fail( "Unexpected result: " + person );
				}
			}
		} );
	}

	@Test
	public void testSelectHierarchyLeaf(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Man> men = session.createQuery( "FROM Man m", Man.class ).getResultList();
			for ( Man man : men ) {
				assertThat( man.getHobby(), is( john.getHobby() ) );
			}
		} );
	}

	@Test
	public void testGetHierarchyLeaf(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Man man = session.get( Man.class, "John" );
			assertThat( man.getHobby(), is( john.getHobby() ) );
		} );
	}


	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
	public static class Person {

		@Id
		private String name;


		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	@Entity(name = "Child")
	@DiscriminatorValue("CHILD")
	public static class Child extends Person {

		private String favouriteToy;

		@ManyToOne
		private Woman mother;

		@ManyToOne
		private Man father;

		public Child() {
		}

		public Child(String name, String favouriteToy) {
			super( name );
			this.favouriteToy = favouriteToy;
		}

		public String getFavouriteToy() {
			return favouriteToy;
		}

		public void setFavouriteToy(String favouriteToy) {
			this.favouriteToy = favouriteToy;
		}

		public Man getFather() {
			return father;
		}

		public void setFather(Man father) {
			this.father = father;
		}

		public Woman getMother() {
			return mother;
		}

		public void setMother(Woman mother) {
			this.mother = mother;
		}
	}

	@Entity(name = "Man")
	@DiscriminatorValue("MAN")
	public static class Man extends Person {

		private String hobby;

		@OneToOne
		private Woman wife;

		@OneToMany(mappedBy = "father")
		private List<Child> children = new ArrayList<>();

		public Man() {
		}

		public Man(String name, String hobby) {
			super( name );
			this.hobby = hobby;
		}

		public String getHobby() {
			return hobby;
		}

		public void setHobby(String hobby) {
			this.hobby = hobby;
		}

		public Woman getWife() {
			return wife;
		}

		public void setWife(Woman wife) {
			this.wife = wife;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Woman")
	@DiscriminatorValue("WOMAN")
	public static class Woman extends Person {

		private String job;

		@OneToOne
		private Man husband;

		@OneToMany(mappedBy = "mother")
		private List<Child> children = new ArrayList<>();

		public Woman() {
		}

		public Woman(String name, String job) {
			super( name );
			this.job = job;
		}

		public String getJob() {
			return job;
		}

		public void setJob(String job) {
			this.job = job;
		}

		public Man getHusband() {
			return husband;
		}

		public void setHusband(Man husband) {
			this.husband = husband;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}
}
