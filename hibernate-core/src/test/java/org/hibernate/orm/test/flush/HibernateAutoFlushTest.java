/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class HibernateAutoFlushTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Advertisement.class
		};
	}

	@Test
	public void testFlushAutoSQLNativeSession() {
		doInHibernate(this::sessionFactory, session -> {
			session.createNativeQuery("delete from Person", Object.class).executeUpdate();
		});
		doInHibernate(this::sessionFactory, session -> {
			log.info("testFlushAutoSQLNativeSession");
			//tag::flushing-auto-flush-sql-native-example[]
			assertTrue( session.createNativeQuery( "select count(*) from Person", Integer.class )
					.getSingleResult() == 0);

			Person person = new Person("John Doe");
			session.persist(person);

			assertTrue( session.createNativeQuery( "select count(*) from Person", Integer.class )
					.uniqueResult() == 0);
			//end::flushing-auto-flush-sql-native-example[]
		});
	}

	//tag::flushing-auto-flush-jpql-entity-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Person() {}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

	@Entity(name = "Advertisement")
	public static class Advertisement {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
	//end::flushing-auto-flush-jpql-entity-example[]
}
