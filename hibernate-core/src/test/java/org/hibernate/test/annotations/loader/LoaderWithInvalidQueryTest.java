/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.loader;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class LoaderWithInvalidQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Override
	public void buildEntityManagerFactory() {
		try {
			super.buildEntityManagerFactory();
		}
		catch (Exception expected) {
			HibernateException rootCause = (HibernateException) ExceptionUtil.rootCause( expected );
			assertTrue(rootCause.getMessage().contains( "could not resolve property: valid" ));
			assertTrue(rootCause.getMessage().contains( "_Person is not mapped" ));
		}
	}

	@Test
	public void test() {
	}


	@Entity(name = "Person")
	@Loader(namedQuery = "invalid_sql")
	@NamedQuery(
		name = "invalid_sql",
		query = "SELECT p " +
				"FROM Person p " +
				"WHERE p.id = ?1 and p.valid = true"
	)
	@NamedQuery(
		name = "another_invalid_sql",
		query = "SELECT p " +
				"FROM _Person p " +
				"WHERE p.id = ?1"
	)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "full_name")
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	//end::sql-custom-crud-example[]

}
