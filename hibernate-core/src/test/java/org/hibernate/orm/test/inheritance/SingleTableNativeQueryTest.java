/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Session;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.JdbcSettings.SHOW_SQL;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@JiraKey("HHH-18610")
public class SingleTableNativeQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put(SHOW_SQL, true);
		options.put(FORMAT_SQL, true);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Toy.class,
			Color.class,
			Family.class,
			Person.class,
			Child.class,
			Man.class,
			Woman.class
		};
	}

	@Test
	public void itShouldGetPersons() {
		doInHibernate(this::entityManagerFactory, session -> {
			loadData(session);
			List<Object> results = session.createNativeQuery("select {p.*} from person p order by p.name", Object.class).addEntity("p", Person.class).list();
			assertThat( results.stream().map(p -> ((Person)p).getName()).collect(Collectors.toList()), contains("Jane", "John", "Mark", "Susan") );
		} );
	}

	@Test
	public void itShouldGetWife() {
		doInHibernate(this::entityManagerFactory, session -> {
			loadData(session);
			List<Object[]> results = session.createNativeQuery("select {m.*}, {w.*} from person m left join person w on m.wife_name = w.name where m.TYPE = 'MAN'", Object[].class)
				.addEntity("m", Person.class)
				.addEntity("w", Person.class)
				.list();
			assertThat(results.size(), is(1));
			assertThat(results.get(0)[0], instanceOf(Man.class));
			assertThat(((Man)results.get(0)[0]).getName(), is("John"));
			assertThat(results.get(0)[1], instanceOf(Woman.class));
			assertThat(((Woman)results.get(0)[1]).getName(), is("Jane"));
		});
	}

	@Test
	public void itShouldGetFamilyMembers() {
		doInHibernate(this::entityManagerFactory, session -> {
			loadData(session);
			List<Object> results = session.createNativeQuery("select {f.*} from family f", Object.class).addEntity("f", Family.class).list();
			Family family = (Family)results.get(0);
			List<Person> members = family.getMembers();
			assertThat( members.size(), is( familyMembers.size() ) );
		} );
	}


	private Toy marioToy;
	private Toy fidgetSpinner;
	private Man john;
	private Woman jane;
	private Child susan;
	private Child mark;
	private Family family;
	private List<Child> children;
	private List<Person> familyMembers;

	public void loadData(Session session) {

		marioToy = new Toy(1L, "Super Mario retro Mushroom");
		fidgetSpinner = new Toy(2L, "Fidget Spinner");
		john = new Man( "John", "Riding Roller Coasters" );
		jane = new Woman( "Jane", "Hippotherapist" );
		susan = new Child( "Susan", marioToy );
		mark = new Child( "Mark", fidgetSpinner );
		family = new Family( "McCloud" );
		children = new ArrayList<>( Arrays.asList( susan, mark ) );
		familyMembers = Arrays.asList( john, jane, susan, mark );


		session.persist(marioToy);
		session.persist(fidgetSpinner);

		jane.setColor(new Color("pink"));
		jane.setHusband( john );
		jane.setChildren( children );

		john.setColor(new Color("blue"));
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

		session.flush();
		session.clear();
	}


	@Embeddable
	public static class Color {
//		@JdbcTypeCode(SqlTypes.JSON)
//		@Column(name = "color", columnDefinition = "json")
		@Column(name = "color")
		private String attributes;

		public Color() {
		}

		public Color(final String attributes) {
			this.attributes = attributes;
		}

		public String getAttributes() {
			return attributes;
		}

		public void setAttributes(final String attributes) {
			this.attributes = attributes;
		}
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


	@Entity(name = "Toy")
	public static class Toy {

		@Id
		private Long id;

		private String name;

//		@OneToMany(mappedBy = "favoriteThing", cascade = CascadeType.ALL, orphanRemoval = true)
//		List<Child> favorite = new ArrayList<>();

		public Toy() {
		}

		public Toy(final Long id, final String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	@Entity(name = "Child")
	@DiscriminatorValue("CHILD")
	public static class Child extends Person {

		@ManyToOne
		@JoinColumn(name="fav_toy")
		private Toy favoriteThing;

		@ManyToOne
		private Woman mother;

		@ManyToOne
		private Man father;

		public Child() {
		}

		public Child(String name, Toy favouriteThing) {
			super( name );
			this.favoriteThing = favouriteThing;
		}

		public Toy getFavoriteThing() {
			return favoriteThing;
		}

		public void setFavoriteThing(Toy favouriteThing) {
			this.favoriteThing = favouriteThing;
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

		private Color color;

		@Column(name="fav_hobby")
		private String favoriteThing;

		@OneToOne
		private Woman wife;

		@OneToMany(mappedBy = "father")
		private List<Child> children = new ArrayList<>();

		public Man() {
		}

		public Man(String name, String favoriteThing) {
			super( name );
			this.favoriteThing = favoriteThing;
		}

		public String getFavoriteThing() {
			return favoriteThing;
		}

		public void setFavoriteThing(String favoriteThing) {
			this.favoriteThing = favoriteThing;
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

		public Color getColor() {
			return color;
		}

		public void setColor(final Color color) {
			this.color = color;
		}
	}

	@Entity(name = "Woman")
	@DiscriminatorValue("WOMAN")
	public static class Woman extends Person {

		private Color color;

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

		public Color getColor() {
			return color;
		}

		public void setColor(final Color color) {
			this.color = color;
		}
	}
}
