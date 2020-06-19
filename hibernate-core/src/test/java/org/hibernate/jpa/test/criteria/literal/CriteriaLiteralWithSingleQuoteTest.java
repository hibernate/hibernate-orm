/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.literal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue( jiraKey = "HHH-14077")
public class CriteriaLiteralWithSingleQuoteTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void literalSingleQuoteTest() throws Exception {

		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> query = cb.createQuery();
					query.select( cb.literal( '\'' ) ).from( Student.class );
					Object object = entityManager.createQuery( query ).getSingleResult();
					assertEquals( "'", object );
				}
		);
	}

	@Test
	public void literalProjectionTest() throws Exception {

		doInJPA(
				this::entityManagerFactory,
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
	@SkipForDialect(value = PostgreSQL81Dialect.class, comment = "PostgreSQL does not support literals in group by statement")
	public void testLiteralProjectionAndGroupBy() throws Exception {
		doInJPA(
				this::entityManagerFactory,
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

	@Before
	public void setupData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					Student student = new Student();
					student.setAValue( "A Value" );
					entityManager.persist( student );
				}
		);
	}

	@After
	public void cleanupData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					entityManager.createQuery( "delete from Student" );
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Student.class };
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
