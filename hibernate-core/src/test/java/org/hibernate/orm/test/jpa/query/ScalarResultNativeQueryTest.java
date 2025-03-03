/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
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
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Person( 1, 29 ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List<String> results = em.createNamedQuery( "personAge", String.class ).getResultList();
		assertEquals( 1, results.size() );
		assertEquals( "29", results.get( 0 ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from Person" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}
}
