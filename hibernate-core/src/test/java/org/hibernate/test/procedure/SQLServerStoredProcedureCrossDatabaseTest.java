/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(SQLServer2012Dialect.class)
@TestForIssue( jiraKey = "HHH-12704" )
public class SQLServerStoredProcedureCrossDatabaseTest extends BaseEntityManagerFunctionalTestCase {

	private final String DATABASE_NAME_TOKEN = "databaseName=";

	private final String DATABASE_NAME = "hibernate_orm_test_sp";


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
			"DROP DATABASE " + DATABASE_NAME,
			"CREATE DATABASE " + DATABASE_NAME
		);

		String url = (String) Environment.getProperties().get( AvailableSettings.URL );

		String[] tokens = url.split( DATABASE_NAME_TOKEN );

		url = tokens[0] + DATABASE_NAME_TOKEN + DATABASE_NAME;

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
	@FailureExpected( jiraKey = "HHH-12704", message = "SQL Server JDBC Driver does not support registering name parameters properly")
	public void testStoredProcedureViaJPANamedParameters() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( DATABASE_NAME + ".dbo.sp_square_number" );
			query.registerStoredProcedureParameter( "inputNumber", Integer.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "outputNumber", Integer.class, ParameterMode.OUT );

			query.setParameter( "inputNumber", 7 );

			query.execute();
			int result = (int) query.getOutputParameterValue( "outputNumber" );
			assertEquals( 49, result );
		} );
	}

	@Test
	public void testStoredProcedureViaJPA() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testStoredProcedureViaJDBC() {
		doInJPA( this::entityManagerFactory, entityManager -> {

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
