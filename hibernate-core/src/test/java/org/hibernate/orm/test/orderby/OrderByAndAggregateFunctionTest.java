/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orderby;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@DomainModel(
		annotatedClasses = {
				OrderByAndAggregateFunctionTest.Parent.class,
				OrderByAndAggregateFunctionTest.Child.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-16189")
public class OrderByAndAggregateFunctionTest {

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(H2Dialect.class)
	public void testSelectWithOrderAndGroupBy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select c.id,c.name, sum(p.number) " +
											"from Parent p " +
											"inner join p.child c " +
											"group by c.id " +
											"order by c.name",
									Object[].class
							)
							.getResultList();
				}
		);
	}

	@Test
	public void testSelectWithOrderAndGroupBy2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select c.id, sum(p.number) " +
											"from Parent p " +
											"inner join p.child c " +
											"group by c.id " +
											"order by c.id",
									Object[].class
							)
							.getResultList();
				}
		);
	}

	@Test
	public void testSelectWithHaving(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select c.id, sum(p.number) " +
											"from Parent p " +
											"inner join p.child c " +
											"group by c.id " +
											"having c.id > 5",
									Object[].class
							)
							.getResultList();
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private String code;

		@ManyToOne
		private Child child;

		@Column(name = "NUMBER_COLUMN")
		int number;
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;
	}

}
