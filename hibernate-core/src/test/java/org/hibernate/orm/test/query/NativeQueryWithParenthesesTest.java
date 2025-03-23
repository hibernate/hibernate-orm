/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.community.dialect.FirebirdDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = NativeQueryWithParenthesesTest.Person.class
)
@SkipForDialect(dialectClass = FirebirdDialect.class, majorVersion = 4, reason = "Firebird 4.0 and earlier don't support simple query grouping")
@SkipForDialect(dialectClass = FirebirdDialect.class, majorVersion = 3, reason = "Firebird 4.0 and earlier don't support simple query grouping")
@SkipForDialect(dialectClass = FirebirdDialect.class, majorVersion = 2, reason = "Firebird 4.0 and earlier don't support simple query grouping")
@SessionFactory
public class NativeQueryWithParenthesesTest {

	@Test
	public void testParseParentheses(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.createNativeQuery(
								"(SELECT p.id, p.name FROM Person p WHERE p.name LIKE 'A%') " +
										"UNION " +
										"(SELECT p.id, p.name FROM Person p WHERE p.name LIKE 'B%')",
								Person.class
						).getResultList()
		);
	}

	@Entity
	@Table(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
