/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.test.idgen.identity.hhh9983;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.Oracle12cDialect;

import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9983")
@RequiresDialect(Oracle12cDialect.class)
public class SaveEntityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Company.class};
	}

	@Test
	public void testSave() {
		Session s = openSession();
		Transaction transaction = s.beginTransaction();
		try {
			s.save( new Company() );
			s.getTransaction().commit();
		}
		finally {
			s.close();
		}
	}

	@Entity
	@Table(name = "Company")
	public class Company {
		private Integer id;
		private String name;

		public Company() {
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
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
