/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.loader;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
			Throwable[] suppressed = rootCause.getSuppressed();
			assertEquals( 2, suppressed.length );
			assertTrue( ExceptionUtil.rootCause( suppressed[0] ).getMessage().contains( "Could not resolve root entity '_Person'" ) );
			assertTrue( ExceptionUtil.rootCause( suppressed[1] ).getMessage().contains( "Could not resolve attribute 'valid'" ) );
		}
	}

	@Test
	public void test() {
	}


	@Entity(name = "Person")
	@HQLSelect(
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
