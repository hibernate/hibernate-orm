/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi.metadatabuildercontributor;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@TestForIssue( jiraKey = "HHH-12589" )
public class SqlFunctionMissingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Employee.class,
		};
	}

	final Employee employee = new Employee();

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			employee.id = 1L;
			employee.username = "user@acme.com";

			entityManager.persist( employee );
		} );
	}

	@Test
	public void test() {
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Number result = (Number) entityManager.createQuery(
					"select INSTR(e.username,'@') " +
					"from Employee e " +
					"where " +
					"	e.id = :employeeId")
				.setParameter( "employeeId", employee.id )
				.getSingleResult();

				fail("Should throw exception!");
			} );
		}
		catch (Exception expected) {
			assertTrue( ExceptionUtil.rootCause( expected ).getMessage().contains( "No data type for node: org.hibernate.hql.internal.ast.tree.MethodNod" ) );
		}
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		private String password;
	}

}
