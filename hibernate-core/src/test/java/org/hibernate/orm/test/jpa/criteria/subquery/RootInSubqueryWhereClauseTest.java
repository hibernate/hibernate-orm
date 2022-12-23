/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;


@Jpa(
		annotatedClasses = {
				RootInSubqueryWhereClauseTest.Person.class,
				RootInSubqueryWhereClauseTest.Address.class
		}
)
@TestForIssue(jiraKey = "HHH-15477")
public class RootInSubqueryWhereClauseTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					Person person = new Person(1, "Andrea");
					Address address = new Address(2, "Gradoli", "Via Roma");
					person.addAddress( address );
					entityManager.persist( address );
					entityManager.persist( person );

					Person anotherPerson = new Person(2, "Luigi");
					entityManager.persist( anotherPerson );
				}
		);
	}

	@Test
	public void testSubquery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					CriteriaQuery<Person> query = builder.createQuery( Person.class );
					Root<Person> person = query.from( Person.class );

					Subquery<String> subQuery = query.subquery( String.class );

					Root<Address> address = subQuery.from( Address.class );
					subQuery.select( address.get( "city" ) ).where( builder.equal( address.get( "person" ), person ) );

					query.where( builder.exists( subQuery ) );

					List<Person> people = entityManager.createQuery( query ).getResultList();
					assertThat(people.size()).isEqualTo( 1 );
					assertThat( people.get( 0 ).getName() ).isEqualTo( "Andrea" );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Integer id;

		String name;

		@OneToMany
		List<Address> addresses =new ArrayList<>();

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addAddress(Address address){
			addresses.add( address );
			address.person = this;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Address> getAddresses() {
			return addresses;
		}
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {
		@Id
		Integer id;

		private String city;

		private String street;

		@ManyToOne
		private Person person;

		public Address() {
		}

		public Address(Integer id, String city, String street) {
			this.id = id;
			this.city = city;
			this.street = street;
		}
	}
}
