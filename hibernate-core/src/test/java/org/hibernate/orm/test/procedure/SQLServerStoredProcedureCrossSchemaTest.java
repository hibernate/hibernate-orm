/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
		}
)
public class SQLServerStoredProcedureCrossSchemaTest {

	@BeforeEach
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
	public void testStoredProcedureViaJPA(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_test.sp_square_number" );

			query.registerStoredProcedureParameter( "inputNumber", Integer.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "outputNumber", Integer.class, ParameterMode.OUT );

			query.setParameter( "inputNumber", 7 );

			query.execute();

			int result = (int) query.getOutputParameterValue( "outputNumber" );
			assertEquals( 49, result );
		} );
	}
}
