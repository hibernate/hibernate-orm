/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Collections;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@JiraKey( value = "HHH-12704" )
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
		}
)
public class SQLServerStoredProcedureCrossDatabaseTest {

	private final String DATABASE_NAME_TOKEN = "databaseName=";

	private final String DATABASE_NAME = "hibernate_orm_test_sp";

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		doInAutoCommit(
			"DROP DATABASE " + DATABASE_NAME,
			"CREATE DATABASE " + DATABASE_NAME
		);

		String url = (String) Environment.getProperties().get( AvailableSettings.URL );

		String[] tokens = url.split( DATABASE_NAME_TOKEN );

		url = tokens[0] + DATABASE_NAME_TOKEN + DATABASE_NAME + ";trustServerCertificate=true";

		doInAutoCommit( Collections.singletonMap( AvailableSettings.URL, url ),
						"DROP PROCEDURE sp_square_number",
						"CREATE PROCEDURE sp_square_number " +
						"   @inputNumber INT, " +
						"   @outputNumber INT OUTPUT " +
						"AS " +
						"BEGIN " +
						"   SELECT @outputNumber = @inputNumber * @inputNumber; " +
						"END"
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12704", reason = "SQL Server JDBC Driver does not support registering name parameters properly")
	public void testStoredProcedureViaJPANamedParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( DATABASE_NAME + ".dbo.sp_square_number" );
			query.registerStoredProcedureParameter( "outputNumber", Integer.class, ParameterMode.OUT );

			query.setParameter( "inputNumber", 7 );

			query.execute();
			int result = (int) query.getOutputParameterValue( "outputNumber" );
			assertEquals( 49, result );
		} );
	}

	@Test
	public void testStoredProcedureViaJPA(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( DATABASE_NAME + ".dbo.sp_square_number" );
			query.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, Integer.class, ParameterMode.OUT );

			query.setParameter( 1, 7 );

			query.execute();
			int result = (int) query.getOutputParameterValue( 2 );
			assertEquals( 49, result );
		} );
	}

	@Test
	public void testStoredProcedureViaJDBC(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.unwrap( Session.class ).doWork( connection -> {
				try (CallableStatement storedProcedure = connection.prepareCall(
						"{ call " + DATABASE_NAME + ".dbo.sp_square_number(?, ?) }" )) {
					try {
						storedProcedure.registerOutParameter( 2, Types.INTEGER );
						storedProcedure.setInt( 1, 7 );
						storedProcedure.execute();
						int result = storedProcedure.getInt( 2 );
						assertEquals( 49, result );
					}
					finally {
						if ( storedProcedure != null ) {
							storedProcedure.close();
						}
					}
				}
			} );
		} );
	}
}
