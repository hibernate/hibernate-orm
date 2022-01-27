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
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Jpa(
		annotatedClasses = {
				CriteriaFunctionParametersBindingTest.Person.class
		}
)
public class CriteriaFunctionParametersBindingTest {


	@Test
	public void testParameterBinding(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> criteriaQuery = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = criteriaQuery.from( Person.class );
					criteriaQuery.where( criteriaBuilder.equal(
							person.get( "name" ),
							criteriaBuilder.substring(
									criteriaBuilder.parameter( String.class, "string" ),
									criteriaBuilder.parameter( Integer.class, "start" )
							)
					) );
					criteriaQuery.select( person );

					final TypedQuery<Person> query = entityManager.createQuery( criteriaQuery );
					query.setParameter( "string", "andrea" );
					query.setParameter( "start", 2 );

					List<Person> people = query.getResultList();

				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;


	}

}
