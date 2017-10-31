/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class NativeQueryWithParenthesesTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
			Person.class
		};
	}

	@Test
	public void testParseParentheses() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery(
				"(SELECT p.id, p.name FROM Person p WHERE p.name LIKE 'A%') " +
				"UNION " +
				"(SELECT p.id, p.name FROM Person p WHERE p.name LIKE 'B%')", Person.class)
			.getResultList();
		} );
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
