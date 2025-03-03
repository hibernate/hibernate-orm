/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = CrtiteriaGetResultTypeTest.Person.class
)
public class CrtiteriaGetResultTypeTest {

	@Test
	public void testObjectResultType(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					final CriteriaQuery query = entityManager.getCriteriaBuilder().createQuery();
					final Class resultType = query.getResultType();

					assertThat( resultType, notNullValue() );
					assertEquals( Object.class.getName(), resultType.getName() );

				}
		);
	}

	@Test
	public void testTupleResultType(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					final CriteriaQuery query = entityManager.getCriteriaBuilder().createQuery( Tuple.class );
					final Class resultType = query.getResultType();

					assertThat( resultType, notNullValue() );
					assertEquals( Tuple.class.getName(), resultType.getName() );

				}
		);
	}


	@Test
	public void testEntityResultType(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					final CriteriaQuery query = entityManager.getCriteriaBuilder().createQuery( Person.class );
					final Class resultType = query.getResultType();

					assertThat( resultType, notNullValue() );
					assertEquals( Person.class.getName(), resultType.getName() );
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
