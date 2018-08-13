/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.type.StringType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL81Dialect.class)
public class PostgreSQLStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );

			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP FUNCTION sp_count_phones(bigint)" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );

			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP FUNCTION fn_phones(bigint)" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );

			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP FUNCTION singleRefCursor(bigint)" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );

			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP FUNCTION sp_is_null()" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );

			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION sp_count_phones( " +
									"   IN personId bigint, " +
									"   OUT phoneCount bigint) " +
									"   RETURNS bigint AS " +
									"$BODY$ " +
									"    BEGIN " +
									"        SELECT COUNT(*) INTO phoneCount " +
									"        FROM phone  " +
									"        WHERE person_id = personId; " +
									"    END; " +
									"$BODY$ " +
									"LANGUAGE plpgsql;"
					);

					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION fn_phones(personId BIGINT) " +
									"   RETURNS REFCURSOR AS " +
									"$BODY$ " +
									"    DECLARE " +
									"        phones REFCURSOR; " +
									"    BEGIN " +
									"        OPEN phones FOR  " +
									"            SELECT *  " +
									"            FROM phone   " +
									"            WHERE person_id = personId;  " +
									"        RETURN phones; " +
									"    END; " +
									"$BODY$ " +
									"LANGUAGE plpgsql"
					);

					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION singleRefCursor() " +
									"   RETURNS REFCURSOR AS " +
									"$BODY$ " +
									"    DECLARE " +
									"        p_recordset REFCURSOR; " +
									"    BEGIN " +
									"      OPEN p_recordset FOR SELECT 1; " +
									"      RETURN p_recordset; " +
									"    END; " +
									"$BODY$ " +
									"LANGUAGE plpgsql;"
					);

					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION sp_is_null( " +
									"   IN param varchar(255), " +
									"   OUT result boolean) " +
									"   RETURNS boolean AS " +
									"$BODY$ " +
									"    BEGIN " +
									"    select param is null into result; " +
									"    END; " +
									"$BODY$ " +
									"LANGUAGE plpgsql;"
					);
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person1 = new Person( "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
														  .toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );

			person1.addPhone( phone2 );
		} );
	}

	@Test
	public void testStoredProcedureOutParameter() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	public void testStoredProcedureRefCursor() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );

			query.setParameter( 2, 1L );

			List<Object[]> phones = query.getResultList();
			assertEquals( 2, phones.size() );
		} );
	}

	@Test
	public void testFunctionWithJDBC() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long phoneCount = session.doReturningWork( connection -> {
				CallableStatement function = null;
				try {
					function = connection.prepareCall( "{ ? = call sp_count_phones(?) }" );
					function.registerOutParameter( 1, Types.BIGINT );
					function.setLong( 2, 1L );
					function.execute();
					return function.getLong( 1 );
				}
				finally {
					if ( function != null ) {
						function.close();
					}
				}
			} );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	public void testFunctionWithJDBCByName() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				Session session = entityManager.unwrap( Session.class );
				Long phoneCount = session.doReturningWork( connection -> {
					CallableStatement function = null;
					try {
						function = connection.prepareCall( "{ ? = call sp_count_phones(?) }" );
						function.registerOutParameter( "phoneCount", Types.BIGINT );
						function.setLong( "personId", 1L );
						function.execute();
						return function.getLong( 1 );
					}
					finally {
						if ( function != null ) {
							function.close();
						}
					}
				} );
				assertEquals( Long.valueOf( 2 ), phoneCount );
			}
			catch (Exception e) {
				assertEquals( SQLFeatureNotSupportedException.class, e.getCause().getClass() );
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11863")
	public void testSysRefCursorAsOutParameter() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long value = null;

			Session session = entityManager.unwrap( Session.class );

			try (ResultSet resultSet = session.doReturningWork( connection -> {
				CallableStatement function = null;
				try {
					function = connection.prepareCall( "{ ? = call singleRefCursor() }" );
					function.registerOutParameter( 1, Types.REF_CURSOR );
					function.execute();
					return (ResultSet) function.getObject( 1 );
				}
				finally {
					if ( function != null ) {
						function.close();
					}
				}
			} )) {
				while ( resultSet.next() ) {
					value = resultSet.getLong( 1 );
				}
			}
			catch (Exception e) {
				fail( e.getMessage() );
			}
			assertEquals( Long.valueOf( 1 ), value );


			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );

			function.execute();

			assertFalse( function.hasMoreResults() );

			value = null;
			try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
				while ( resultSet.next() ) {
					value = resultSet.getLong( 1 );
				}
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}

			assertEquals( Long.valueOf( 1 ), value );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12905")
	public void testStoredProcedureNullParameterHibernate() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StringType.class, ParameterMode.IN ).enablePassingNulls( true );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.OUT );
			procedureCall.setParameter( 1, null );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertTrue( result );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StringType.class, ParameterMode.IN ).enablePassingNulls( true );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.OUT );
			procedureCall.setParameter( 1, "test" );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertFalse( result );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12905")
	public void testStoredProcedureNullParameterHibernateWithoutEnablePassingNulls() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				ProcedureCall procedureCall = entityManager.unwrap( Session.class )
				.createStoredProcedureCall( "sp_is_null" );
				procedureCall.registerParameter( "param", StringType.class, ParameterMode.IN );
				procedureCall.registerParameter( "result", Boolean.class, ParameterMode.OUT );
				procedureCall.setParameter( "param", null );

				procedureCall.getOutputParameterValue( "result" );

				fail("Should have thrown exception");
			}
			catch (IllegalArgumentException e) {
				assertEquals( "The parameter named [param] was null. You need to call ParameterRegistration#enablePassingNulls(true) in order to pass null parameters.", e.getMessage() );
			}
		} );
	}
}
