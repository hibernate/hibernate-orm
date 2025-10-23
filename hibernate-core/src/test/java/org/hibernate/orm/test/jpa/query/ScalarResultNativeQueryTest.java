/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * Tests for selecting scalar value from native queries.
 *
 * @author Gunnar Morling
 */
@RequiresDialect(H2Dialect.class)
@Jpa(annotatedClasses = {ScalarResultNativeQueryTest.Person.class})
public class ScalarResultNativeQueryTest {

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

	@Test
	public void shouldApplyConfiguredTypeForProjectionOfScalarValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new Person( 1, 29 ) ) );

		scope.inTransaction( entityManager -> {
			List<String> results = entityManager.createNamedQuery( "personAge", String.class ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( "29", results.get( 0 ) );
		} );

		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from Person" ).executeUpdate() );
	}
}
