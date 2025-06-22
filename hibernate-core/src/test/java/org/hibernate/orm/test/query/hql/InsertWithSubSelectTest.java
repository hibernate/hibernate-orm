/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author bjoern.moritz
 */
@JiraKey(value = "HHH-5274")
@DomainModel(
		annotatedClasses = {
				InsertWithSubSelectTest.A.class,
				InsertWithSubSelectTest.B.class,
				InsertWithSubSelectTest.C.class
		}
)
@SessionFactory
public class InsertWithSubSelectTest {
	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createMutationQuery(
							"insert into C (id) " +
									"select a.id from A a " +
									"where exists (" +
									"	select 1 " +
									"	from B b " +
									"	where b.id = a.id" +
									")"
					)
							.executeUpdate();
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry = "select a.id " +
							"from A a " +
							"where exists (" +
							"	select 1 " +
							"	from B b " +
							"	where b.id = a.id" +
							")";
					session.createQuery( qry ).getResultList();
				}
		);
	}

	@Entity(name = "A")
	public static class A {

		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "B")
	public static class B {

		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "C")
	public static class C {

		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
