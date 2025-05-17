/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;


import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.type.StandardBasicTypes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		standardModels = StandardDomainModel.LIBRARY
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19383")
public class NativeQueryResultCheckingTests {

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19376")
	public void testForHHH19376(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String sql = "SELECT p.*, COUNT(*) OVER() AS total_count "
								+ "FROM Person p "
								+ "WHERE p.name ILIKE :name "
								+ "ORDER BY p.id";
					// Declare Person as result type
					NativeQuery<Person> query = session.createNativeQuery(sql, Person.class);
					query.setParameter("name", "Ga%");
					query.setMaxResults(2);
					query.setFirstResult(0);
					// Now mutate the result set mapping and verify an Exception is thrown
					assertThrows( IllegalArgumentException.class,
							() -> query.addScalar( "total_count", StandardBasicTypes.LONG).list() );
				}
		);
	}

	@Test
	public void testMutateResultSetMapping1(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertThrows( IllegalArgumentException.class,
								() -> session.createNativeQuery( "select title, isbn from Book", String.class )
										.addScalar( "title", String.class )
										.addScalar( "isbn", String.class )
										.getResultList()
				)
		);
	}

	@Test
	public void testMutateResultSetMapping2(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertThrows( IllegalArgumentException.class,
								() -> session.createNativeQuery( "select title, isbn from Book", Book.class )
										// this mapping doesn't have an appropriate constructor in Book, should throw error
										.addScalar( "title", String.class )
										.addScalar( "isbn", String.class )
										.getResultList()
						)
		);
	}

	@Test
	public void testRecordWithPrimitiveField(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
					assertDoesNotThrow(
							() -> session.createNativeQuery( "select id, name from Person", Record.class)
									// map a Long onto a primitive long, shouldn't throw an exception
									.addScalar("id", Long.class)
									.addScalar("name", String.class)
									.getResultList()
					)
		);
	}

	static class Record {
		long id;
		String name;
		public Record(long id, String name) {
			this.id = id;
			this.name = name;
		}
		long id() {
			return id;
		}
		String name() {
			return name;
		}
	}

}
