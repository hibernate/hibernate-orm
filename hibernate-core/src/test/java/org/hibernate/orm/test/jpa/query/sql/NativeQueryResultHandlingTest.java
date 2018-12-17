/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query.sql;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.EntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12780" )
@RequiresDialect(H2Dialect.class)
public class NativeQueryResultHandlingTest extends EntityManagerFactoryBasedFunctionalTest {

	@Entity(name="Person")
	@Table(name="person")
	public static class Person {

		@Id
		private Integer id;

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

	@BeforeEach
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Person( 1, 29 ) );
		} );
	}

	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testNativeSQLWithExplicitScalarMapping() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Integer> results = entityManager.createNativeQuery(
				"select p.age from person p", Integer.class )
			.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( Integer.valueOf( 29 ), results.get( 0 ) );
		} );
	}

	@Test
	public void testNativeSQLWithExplicitTypedArrayMapping() {
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
	public void testNativeSQLWithObjectArrayMapping() {
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
