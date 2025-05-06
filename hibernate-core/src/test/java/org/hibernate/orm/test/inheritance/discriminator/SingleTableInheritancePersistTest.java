/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Davide D'Alto
 */
@JiraKey(value = "HHH-12332")
@DomainModel(
		annotatedClasses = {
				SingleTableInheritancePersistTest.Family.class,
				SingleTableInheritancePersistTest.Person.class,
				SingleTableInheritancePersistTest.Child.class,
				SingleTableInheritancePersistTest.Man.class,
				SingleTableInheritancePersistTest.Woman.class
		}
)
@SessionFactory
public class SingleTableInheritancePersistTest {
	private final Man john = new Man( "John", "Riding Roller Coasters" );

	private final Woman jane = new Woman( "Jane", "Hippotherapist" );
	private final Child susan = new Child( "Susan", "Super Mario retro Mushroom" );
	private final Child mark = new Child( "Mark", "Fidget Spinner" );
	private final Family family = new Family( "McCloud" );
	private final List<Child> children = new ArrayList<>( Arrays.asList( susan, mark ) );
	private final List<Person> familyMembers = Arrays.asList( john, jane, susan, mark );

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					jane.setHusband( john );
					jane.setChildren( children );

					john.setWife( jane );
					john.setChildren( children );

					for ( Child child : children ) {
						child.setFather( john );
						child.setMother( jane );
					}

					for ( Person person : familyMembers ) {
						family.add( person );
					}

					session.persist( family );
				} );
	}

	@Test
	public void testPolymorphicAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Family family = session.createQuery( "FROM Family f", Family.class ).getSingleResult();
					List<Person> members = family.getMembers();
					assertThat( members.size(), is( familyMembers.size() ) );
					for ( Person person : members ) {
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
	@JiraKey(value = "HHH-15497")
	public void testFetchChildrenCountTwiceFails(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<Long> query = session.createQuery(
							"SELECT count(p) FROM Person p WHERE TYPE(p) = ?1",
							Long.class
					);
					query.setParameter( 1, Child.class );
					Long personCount = query.getSingleResult();

					assertThat( personCount, is( 2L ) );

					query = session.createQuery( "SELECT count(p) FROM Person p WHERE TYPE(p) = ?1", Long.class );
					query.setParameter( 1, Child.class );
					personCount = query.getSingleResult();

					assertThat( personCount, is( 2L ) );
				} );
	}

	@Entity(name = "Family")
	public static class Family {

		@Id
		private String name;

		@OneToMany(mappedBy = "familyName", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Person> members = new ArrayList<>();

		public Family() {
		}

		public Family(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Person> getMembers() {
			return members;
		}

		public void setMembers(List<Person> members) {
			this.members = members;
		}

		public void add(Person person) {
			person.setFamilyName( this );
			members.add( person );
		}

		@Override
		public String toString() {
			return "Family [name=" + name + "]";
		}
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
	public static class Person {

		@Id
		private String name;

		@ManyToOne
		private Family familyName;

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

		public Family getFamilyName() {
			return familyName;
		}

		public void setFamilyName(Family familyName) {
			this.familyName = familyName;
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
