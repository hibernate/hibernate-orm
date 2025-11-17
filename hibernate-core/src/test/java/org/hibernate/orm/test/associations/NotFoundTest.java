/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Fábio Ueno
 */
@Jpa(
		annotatedClasses = {
				NotFoundTest.Person.class,
				NotFoundTest.City.class
		},
		useCollectingStatementInspector = true
)
public class NotFoundTest {

	@BeforeEach
	public void createTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					//tag::associations-not-found-persist-example[]
					City newYork = new City( 1, "New York" );
					entityManager.persist( newYork );

					Person person = new Person( 1, "John Doe", newYork );
					entityManager.persist( person );
					//end::associations-not-found-persist-example[]
				}
		);
	}

	@AfterEach
	public void dropTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					//tag::associations-not-found-find-baseline[]
					Person person = entityManager.find( Person.class, 1 );
					assertEquals( "New York", person.getCity().getName() );
					//end::associations-not-found-find-baseline[]
				}
		);

		breakForeignKey( scope );

		scope.inEntityManager(
				entityManager -> {
					//tag::associations-not-found-non-existing-find-example[]
					Person person = entityManager.find( Person.class, 1 );

					assertNull( person.getCity(), "person.getCity() should be null" );
					//end::associations-not-found-non-existing-find-example[]
				}
		);
	}

	@Test
	public void queryTest(EntityManagerFactoryScope scope) {
		breakForeignKey( scope );

		scope.inTransaction(
				entityManager -> {
					//tag::associations-not-found-implicit-join-example[]
					final List<Person> nullResults = entityManager
							.createQuery( "from Person p where p.city.id is null", Person.class )
							.getResultList();
					assertThat( nullResults ).isEmpty();

					final List<Person> nonNullResults = entityManager
							.createQuery( "from Person p where p.city.id is not null", Person.class )
							.getResultList();
					assertThat( nonNullResults ).isEmpty();
					//end::associations-not-found-implicit-join-example[]
				}
		);
	}

	@Test
	public void queryTestFk(EntityManagerFactoryScope scope) {
		breakForeignKey( scope );
		final SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				entityManager -> {
					sqlStatementInspector.clear();
					//tag::associations-not-found-fk-function-example[]
					final List<String> nullResults = entityManager
							.createQuery( "select p.name from Person p where fk( p.city ) is null", String.class )
							.getResultList();

					assertThat( nullResults ).isEmpty();

					final List<String> nonNullResults = entityManager
							.createQuery( "select p.name from Person p left join p.city c where fk( c ) is not null", String.class )
							.getResultList();
					assertThat( nonNullResults ).hasSize( 1 );
					assertThat( nonNullResults.get( 0 ) ).isEqualTo( "John Doe" );
					//end::associations-not-found-fk-function-example[]

					// In addition, make sure that the two executed queries do not create a join
					assertThat( sqlStatementInspector.getSqlQueries().size() ).isEqualTo( 2 );
					assertThat( sqlStatementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
					assertThat( sqlStatementInspector.getSqlQueries().get( 1 ) ).doesNotContain( " join " );
				}
		);
	}

	private void breakForeignKey(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					//tag::associations-not-found-break-fk[]
					// the database allows this because there is no physical foreign-key
					entityManager.createQuery( "delete City" ).executeUpdate();
					//end::associations-not-found-break-fk[]
				}
		);
	}

	//tag::associations-not-found-domain-model-example[]
	@Entity(name = "Person")
	@Table(name = "Person")
	public static class Person {

		@Id
		private Integer id;
		private String name;

		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "city_fk", referencedColumnName = "id")
		private City city;

		//Getters and setters are omitted for brevity

	//end::associations-not-found-domain-model-example[]


		public Person() {
		}

		public Person(Integer id, String name, City city) {
			this.id = id;
			this.name = name;
			this.city = city;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public City getCity() {
			return city;
		}
	//tag::associations-not-found-domain-model-example[]
	}

	@Entity(name = "City")
	@Table(name = "City")
	public static class City implements Serializable {

		@Id
		private Integer id;

		private String name;

		//Getters and setters are omitted for brevity

	//end::associations-not-found-domain-model-example[]


		public City() {
		}

		public City(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	//tag::associations-not-found-domain-model-example[]
	}
	//end::associations-not-found-domain-model-example[]
}
