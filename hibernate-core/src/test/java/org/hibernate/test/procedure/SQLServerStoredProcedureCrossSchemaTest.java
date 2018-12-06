/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

import java.sql.SQLException;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(SQLServer2012Dialect.class)
public class SQLServerStoredProcedureCrossSchemaTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
		};
	}

	@Before
	public void init() {
		doInAutoCommit(
			"DROP PROCEDURE sp_test.sp_square_number",
			"DROP SCHEMA sp_test",
			"CREATE SCHEMA sp_test",
			"CREATE PROCEDURE sp_test.sp_square_number " +
			"   @inputNumber INT, " +
			"   @outputNumber INT OUTPUT " +
			"AS " +
			"BEGIN " +
			"   SELECT @outputNumber = @inputNumber * @inputNumber; " +
			"END"
		);
	}

	@Test
	public void testStoredProcedureViaJPA() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_test.sp_square_number");

			query.registerStoredProcedureParameter("inputNumber", Integer.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("outputNumber", Integer.class, ParameterMode.OUT);

			query.setParameter("inputNumber", 7);

			query.execute();

			int result = (int) query.getOutputParameterValue("outputNumber");
			assertEquals( 49, result );
		} );
	}
}
