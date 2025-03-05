/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				OneToManySubSelectFetchAndInheritanceTest.Parent.class,
				OneToManySubSelectFetchAndInheritanceTest.AnotherParent.class,
				OneToManySubSelectFetchAndInheritanceTest.SomeOtherParent.class,
				OneToManySubSelectFetchAndInheritanceTest.Another.class,
				OneToManySubSelectFetchAndInheritanceTest.SomeOther.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16258")
public class OneToManySubSelectFetchAndInheritanceTest {

	public static final String ANOTHER_PARENT_NAME = "another parent";
	public static final String SOME_OTHER_PARENT_NAME = "some other";
	public static final String ANOTHER_NAME = "another";
	public static final String SOME_OTHER_NAME = "some other";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Another another = new Another( ANOTHER_NAME );
					AnotherParent anotherParent = new AnotherParent( 1, ANOTHER_PARENT_NAME );

					SomeOtherParent someOtherParent = new SomeOtherParent( 2, SOME_OTHER_PARENT_NAME );
					anotherParent.addAnotherEntity( another );

					session.persist( anotherParent );
					session.persist( someOtherParent );
				}
		);
	}

	@Test
	public void testInitializeCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Parent> parents = session.createQuery(
							"from Parent p order by p.id",
							Parent.class
					).list();

					assertThat( parents ).hasSize( 2 );
					assertThat( parents.get( 0 ) ).isInstanceOf( AnotherParent.class );

					AnotherParent anotherParent = (AnotherParent) parents.get( 0 );
					assertThat( anotherParent.getName() ).isEqualTo( ANOTHER_PARENT_NAME );

					List<Another> anothers = anotherParent.getAnothers();
					assertThat( anothers ).hasSize( 1 );

					Another another = anothers.get( 0 );

					assertThat( another.getName() ).isEqualTo( ANOTHER_NAME );
					assertThat( another.getParent() ).isEqualTo( anotherParent );

					assertThat( parents.get( 1 ) ).isInstanceOf( SomeOtherParent.class );

					SomeOtherParent someOtherParent = (SomeOtherParent) parents.get( 1 );
					assertThat( someOtherParent.getName() ).isEqualTo( SOME_OTHER_PARENT_NAME );
					List<SomeOther> someOthers = someOtherParent.getSomeOthers();
					assertThat( someOthers ).hasSize( 0 );

					someOtherParent.addSomeOtherEntity( new SomeOther( SOME_OTHER_NAME ) );
				}
		);

		scope.inTransaction(
				session -> {
					List<Parent> parents = session.createQuery(
							"from Parent p order by p.id",
							Parent.class
					).list();

					assertThat( parents ).hasSize( 2 );
					assertThat( parents.get( 0 ) ).isInstanceOf( AnotherParent.class );

					AnotherParent anotherParent = (AnotherParent) parents.get( 0 );
					assertThat( anotherParent.getName() ).isEqualTo( ANOTHER_PARENT_NAME );
					List<Another> anothers = anotherParent.getAnothers();
					assertThat( anothers ).hasSize( 1 );

					Another another = anothers.get( 0 );

					assertThat( another.getName() ).isEqualTo( ANOTHER_NAME );
					assertThat( another.getParent() ).isEqualTo( anotherParent );

					assertThat( parents.get( 1 ) ).isInstanceOf( SomeOtherParent.class );

					SomeOtherParent someOtherParent = (SomeOtherParent) parents.get( 1 );
					assertThat( someOtherParent.getName() ).isEqualTo( SOME_OTHER_PARENT_NAME );
					List<SomeOther> someOthers = someOtherParent.getSomeOthers();
					assertThat( someOthers ).hasSize( 1 );

					SomeOther someOther = someOthers.get( 0 );
					assertThat( someOther.getName() ).isEqualTo( SOME_OTHER_NAME );
					assertThat( someOther.getParent() ).isEqualTo( someOtherParent );
				}
		);
	}


	@Entity(name = "Parent")
	@Table(name = "PARENT_TABLE")
	@DiscriminatorColumn(name = "DISC_COL", discriminatorType = DiscriminatorType.INTEGER)
	@DiscriminatorValue("0")
	public static class Parent {

		@Id
		Integer id;

		String name;

		public Parent() {
		}

		public Parent(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "AnotherParent")
	@DiscriminatorValue("1")
	public static class AnotherParent extends Parent {

		@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
		@Fetch(FetchMode.SUBSELECT)
		List<Another> anothers = new ArrayList<>();

		public AnotherParent() {
		}

		public AnotherParent(Integer id, String name) {
			super( id, name );
		}

		public List<Another> getAnothers() {
			return anothers;
		}

		public void addAnotherEntity(Another another) {
			this.anothers.add( another );
			another.parent = this;
		}
	}

	@Entity(name = "SomeOtherParent")
	@DiscriminatorValue("2")
	public static class SomeOtherParent extends Parent {

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@Fetch(FetchMode.SUBSELECT)
		List<SomeOther> someOthers = new ArrayList<>();

		public SomeOtherParent() {
		}

		public SomeOtherParent(Integer id, String name) {
			super( id, name );
		}

		public List<SomeOther> getSomeOthers() {
			return someOthers;
		}

		public void addSomeOtherEntity(SomeOther someOther) {
			this.someOthers.add( someOther );
			someOther.parent = this;
		}
	}

	@Entity(name = "Another")
	@Table(name = "ANOTHER_TABLE")
	public static class Another {

		@Id
		@GeneratedValue
		Integer id;

		String name;

		@ManyToOne
		Parent parent;

		public Another() {
		}

		public Another(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}
	}

	@Entity(name = "SomeOther")
	@Table(name = "SOMEOTHER_TABLE")
	public static class SomeOther {

		@Id
		@GeneratedValue
		Integer id;

		String name;

		@ManyToOne
		Parent parent;

		public SomeOther() {
		}

		public SomeOther(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}
	}

}
