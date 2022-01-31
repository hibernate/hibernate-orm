/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.compliance;


import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = NamedQueryTest.Person.class
)
public class NamedQueryTest {


	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person1 = new Person( 1, "Andrea" );
					Person person2 = new Person( 2, "Alberto" );

					entityManager.persist( person1 );
					entityManager.persist( person2 );
				}
		);
	}

	@Test
	public void testNameQueryCreationFromCritera(EntityManagerFactoryScope scope) {

		final EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();

		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManagerFactory.getCriteriaBuilder();
					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> person = query.from( Person.class );

					query.select( person.get( "id" ) );
					query.where( criteriaBuilder.equal( person.get( "name" ), "Alberto" ) );

					entityManagerFactory.addNamedQuery( "criteria_query", entityManager.createQuery( query ) );

					List<Integer> ids = entityManager.createNamedQuery( "criteria_query", Integer.class )
							.getResultList();
					assertEquals( 1, ids.size() );
					assertEquals( 2, ids.get( 0 ) );
				}
		);

	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
