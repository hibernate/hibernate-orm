/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Davide D'Alto
 */
@TestForIssue( jiraKey = "HHH-12332")
public class TablePerClassInheritancePersistTest extends BaseCoreFunctionalTestCase {
	private final Man john = new Man( "John", "Riding Roller Coasters" );

	private final Woman jane = new Woman( "Jane", "Hippotherapist" );
	private final Child susan = new Child( "Susan", "Super Mario retro Mushroom" );
	private final Child mark = new Child( "Mark", "Fidget Spinner" );
	private final Family family = new Family( "McCloud" );
	private final List<Child> children = new ArrayList<>( Arrays.asList( susan, mark ) );
	private final List<Person> familyMembers = Arrays.asList( john, jane, susan, mark );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Family.class,
				Person.class,
				Child.class,
				Man.class,
				Woman.class
		};
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
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
	public void testPolymorphicAssociation() {
		doInHibernate( this::sessionFactory, session1 -> {
			Family family = session1.createQuery( "FROM Family f", Family.class ).getSingleResult();
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
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
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

		@OneToOne
		private Woman mother;

		@OneToOne
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
