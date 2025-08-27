/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableAsParameterTest.Person.class,
				EmbeddableAsParameterTest.EntityTest.class
		}
)
@SessionFactory
public class EmbeddableAsParameterTest {

	@Test
	public void testAsParameterInWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select p from Person p where p.name = :name" ).
							setParameter( "name", new Name( "Fab", "Fab" ) ).list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testAsParameterReuseInWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select p from Person p where p.name = :name or p.name = :name " )
							.
									setParameter( "name", new Name( "Fab", "Fab" ) )
							.list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testAsParameterReuseInWhereClause2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select p from Person p where p.embeddableTest = :embeddable" ).
							setParameter( "embeddable", new EmbeddableTest() ).list();
					assertThat( results.size(), is( 0 ) );
				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person(
							1,
							new Name( "Fab", "Fab" ),
							33

					);
					session.persist( person );
				} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private Name name;

		private EmbeddableTest embeddableTest;

		private Integer age;

		public Person() {
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public Person(Integer id, Name name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public EmbeddableTest getEmbeddableTest() {
			return embeddableTest;
		}

		public void setEmbeddableTest(EmbeddableTest embeddableTest) {
			this.embeddableTest = embeddableTest;
		}
	}

	@Embeddable
	public static class EmbeddableTest {
		private String street;

		@OneToMany
		private List<EntityTest> entityTests;

	}

	@Entity(name = "EmbeddableTest")
	public static class EntityTest {
		@Id
		private Long id;
		private String name;

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
	}


	@Embeddable
	public static class Name {
		private String firstName;
		private String secondName;

		public Name() {
		}

		public Name(String firstName, String secondName) {
			this.firstName = firstName;
			this.secondName = secondName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getSecondName() {
			return secondName;
		}

		public void setSecondName(String secondName) {
			this.secondName = secondName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}
}
