/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author bjoern.moritz
 */
@TestForIssue(jiraKey = "HHH-5274")
public class InsertWithSubSelectTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			A.class,
			B.class,
			C.class
		};
	}

	@Test
	public void testInsert() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery(
				"insert into C (id) " +
				"select a.id from A a " +
				"where exists (" +
				"	select 1 " +
				"	from B b " +
				"	where b.id = a.id" +
				")"
			)
			.executeUpdate();
		} );
	}

	@Test
	public void testSelect() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery(
				"select a.id " +
				"from A a " +
				"where exists (" +
				"	select 1 " +
				"	from B b " +
				"	where b.id = a.id" +
				")"
			)
			.getResultList();
		} );
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
