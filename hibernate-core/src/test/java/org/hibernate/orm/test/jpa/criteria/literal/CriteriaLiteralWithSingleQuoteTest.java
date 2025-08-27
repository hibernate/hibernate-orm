/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.literal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Jpa(
		annotatedClasses = {
				CriteriaLiteralWithSingleQuoteTest.Student.class
		}
)
public class CriteriaLiteralWithSingleQuoteTest {

	@Test
	public void literalSingleQuoteTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> query = cb.createQuery();
					query.select( cb.literal( '\'' ) ).from( Student.class );
					Object object = entityManager.createQuery( query ).getSingleResult();
					assertEquals( '\'', object );
				}
		);
	}

	@Test
	public void literalProjectionTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> query = cb.createQuery();
					query.multiselect( cb.literal( "' || aValue || '" ) ).from( Student.class );
					Object object = entityManager.createQuery( query ).getSingleResult();
					assertEquals( "' || aValue || '", object );
				}
		);
	}

	@Test
	public void testLiteralProjectionAndGroupBy(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					final String literal = "' || aValue || '";

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> query = cb.createQuery();
					query.multiselect( cb.literal( literal ) )
							.from( Student.class );
					query.groupBy( cb.literal( literal ) );

					Object object = entityManager.createQuery( query ).getSingleResult();
					assertEquals( literal, object );
				}
		);
	}

	@BeforeEach
	public void setupData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Student student = new Student();
					student.setAValue( "A Value" );
					entityManager.persist( student );
				}
		);
	}

	@AfterEach
	public void cleanupData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Student")
	@Table(name = "Students")
	public static class Student {

		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String aValue;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAValue() {
			return aValue;
		}

		public void setAValue(String value) {
			this.aValue = value;
		}
	}
}
