/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.sequence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mhalcea
 */
@RequiresDialect(jiraKey = "HHH-13202", value = PostgreSQL10Dialect.class)
public class PostgreSQLIdentitySupportTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Role.class };
	}

	@Test
	public void test() {
		Role _role = doInJPA( this::entityManagerFactory, entityManager -> {
			Role role = new Role();

			entityManager.persist( role );

			return role;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Role role = entityManager.find( Role.class, _role.getId() );
			assertNotNull(role);
		} );
	}

	@Entity(name = "Role")
	public static class Role {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}

}
