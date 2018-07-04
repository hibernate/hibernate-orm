/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Tests for selecting scalar value from native queries.
 *
 * @author Gunnar Morling
 */
@RequiresDialect(H2Dialect.class)
public class ScalarResultNativeQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Entity(name="Person")
	@Table(name="person")
	@NamedNativeQuery(name = "personAge", query = "select p.age from person p", resultSetMapping = "ageStringMapping")
	@SqlResultSetMapping(name = "ageStringMapping", columns = { @ColumnResult(name = "age", type = String.class) })
	public static class Person {

		@Id
		private Integer id;

		@SuppressWarnings("unused")
		@Column(name = "age")
		private int age;

		public Person() {
		}

		public Person(Integer id, int age) {
			this.id = id;
			this.age = age;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void shouldApplyConfiguredTypeForProjectionOfScalarValue() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Person( 1, 29 ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<String> results = entityManager.createNamedQuery( "personAge", String.class ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( "29", results.get( 0 ) );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12670" )
	public void testNativeSQLWithExplicitScalarMapping() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Person( 1, 29 ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Integer> results = entityManager.createNativeQuery(
				"select p.age from person p", Integer.class )
			.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( Integer.valueOf( 29 ), results.get( 0 ) );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12670" )
	public void testNativeSQLWithExplicitTypedArrayMapping() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Person( 1, 29 ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Integer[]> results = entityManager.createNativeQuery(
					"select p.id, p.age from person p", Integer[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( Integer.valueOf( 1 ), results.get( 0 )[0] );
			assertEquals( Integer.valueOf( 29 ), results.get( 0 )[1] );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12670" )
	public void testNativeSQLWithObjectArrayMapping() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Person( 1, 29 ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Object[]> results = entityManager.createNativeQuery(
					"select p.id, p.age from person p", Object[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( Integer.valueOf( 1 ), results.get( 0 )[0] );
			assertEquals( Integer.valueOf( 29 ), results.get( 0 )[1] );
		} );
	}
}
