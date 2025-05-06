/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

@Jpa(
		annotatedClasses = CriteriaUpdateWithParametersTest.Person.class
)
public class CriteriaUpdateWithParametersTest {

	@Test
	public void testCriteriaUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaUpdate<Person> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Person.class );
					final Root<Person> root = criteriaUpdate.from( Person.class );

					final ParameterExpression<Integer> intValueParameter = criteriaBuilder.parameter( Integer.class );
					final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

					final EntityType<Person> personEntityType = entityManager.getMetamodel().entity( Person.class );

					criteriaUpdate.set(
							root.get( personEntityType.getSingularAttribute( "age", Integer.class ) ),
							intValueParameter
					);
					criteriaUpdate.where( criteriaBuilder.equal(
							root.get( personEntityType.getSingularAttribute( "name", String.class ) ),
							stringValueParameter
					) );

					final Query query = entityManager.createQuery( criteriaUpdate );
					query.setParameter( intValueParameter, 9 );
					query.setParameter( stringValueParameter, "Luigi" );

					query.executeUpdate();
				}
		);
	}

	@Test
	public void testCriteriaUpdate2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaUpdate<Person> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Person.class );
					final Root<Person> root = criteriaUpdate.from( Person.class );

					final ParameterExpression<Integer> intValueParameter = criteriaBuilder.parameter( Integer.class );
					final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

					criteriaUpdate.set( "age", intValueParameter );
					criteriaUpdate.where( criteriaBuilder.equal( root.get( "name" ), stringValueParameter ) );

					final Query query = entityManager.createQuery( criteriaUpdate );
					query.setParameter( intValueParameter, 9 );
					query.setParameter( stringValueParameter, "Luigi" );

					query.executeUpdate();
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private String id;

		private String name;

		private Integer age;

		public Person() {
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAge() {
			return age;
		}
	}
}
