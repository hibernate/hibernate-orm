/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.assertj.core.data.Offset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = PowerFunctionTest.Person.class
)
@JiraKey(value = "HHH-15395")
public class PowerFunctionTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person = new Person( 1L, "And", 50 );
					entityManager.persist( person );
				}
		);
	}

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Double> query = cb.createQuery( Double.class );
					Root<Person> root = query.from( Person.class );

					query.select( cb.power( root.get( "age" ), 2 ) );

					List<Double> results = entityManager.createQuery( query ).getResultList();

					assertThat( results.size() ).isEqualTo( 1 );

					if ( getDialect( scope ) instanceof DerbyDialect ) {
						// for Derby dialect we are emulating the power function see {@link CommonFunctionFactory#power_expLn()}.
						assertThat( results.get( 0 ) ).isEqualTo( 2500D, Offset.offset( 0.000000000001 ) );
					}
					else {
						assertThat( results.get( 0 ) ).isEqualTo( 2500D );
					}
				}
		);
	}

	private Dialect getDialect(EntityManagerFactoryScope scope) {
		return ((SessionFactoryImplementor) scope.getEntityManagerFactory()).getJdbcServices().getDialect();
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;

		private String name;

		private int age;

		public Person() {
		}

		public Person(Long id, String name, int age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

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

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

	}
}
