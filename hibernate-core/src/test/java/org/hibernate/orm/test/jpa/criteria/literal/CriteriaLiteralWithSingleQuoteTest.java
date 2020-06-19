package org.hibernate.orm.test.jpa.criteria.literal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CriteriaLiteralWithSingleQuoteTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Student.class };
	}

	@Test
	public void literalSingleQuoteTest() {

		inTransaction(
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
	public void literalProjectionTest() {

		inTransaction(
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
	public void testLiteralProjectionAndGroupBy() {
		inTransaction(
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
	public void setupData() {
		inTransaction(
				entityManager -> {
					Student student = new Student();
					student.setAValue( "A Value" );
					entityManager.persist( student );
				}
		);
	}

	@AfterEach
	public void cleanupData() {
		inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Student" );
				}
		);
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