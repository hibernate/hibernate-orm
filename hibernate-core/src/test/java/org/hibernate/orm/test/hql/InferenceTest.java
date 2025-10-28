/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = InferenceTest.Person.class)
@SessionFactory
public class InferenceTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var person = new Person( 1, "Johannes", "Buehler" );
			session.persist( person );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testBinaryArithmeticInference(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			List<Person> resultList = session.createQuery( "from Person p where p.id + 1 < :param", Person.class )
					.setParameter("param", 10)
					.getResultList();
			assertThat( resultList ).map( Person::getId ).contains( 1 );
		} );

	}

	@Test
	@JiraKey("HHH-17386")
	public void testInferenceSourceResetForOnClause(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var hql = """
					from Person p
					where p in (
						select p2
						from Person p2
							join Person p3
								on exists (select 1 from Person p4)
					)
					""";
			session.createQuery( hql, Person.class ).getResultList();
		} );

	}

	@Test
	@JiraKey("HHH-18046")
	@SkipForDialect( dialectClass = CockroachDialect.class, reason = "CockroachDB doesn't support multiplication between int and float columns" )
	public void testBinaryArithmeticParameterInference(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			JpaCriteriaQuery<Double> cq = cb.createQuery( Double.class );
			JpaRoot<Person> root = cq.from( Person.class );
			cq.select( cb.toDouble( cb.prod( root.get( "id" ), 0.5f ) ) );
			Double result = session.createQuery( cq ).getSingleResult();
			assertThat( result ).isEqualTo( 0.5d );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;
		private String name;
		private String surname;

		public Person() {
		}

		public Person(Integer id, String name, String surname) {
			this.id = id;
			this.name = name;
			this.surname = surname;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSurname() {
			return surname;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Person )) return false;

			Person person = (Person) o;

			return id != null ? id.equals(person.id) : person.id == null;

		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

}
