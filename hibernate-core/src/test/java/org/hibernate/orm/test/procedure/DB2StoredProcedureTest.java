/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.procedure;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		Person.class,
		Phone.class,
		Vote.class,
		DB2StoredProcedureTest.IdHolder.class,
} )
@RequiresDialect( value = DB2Dialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18332" )
public class DB2StoredProcedureTest {
	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.OUT );

			query.setParameter( 1, 1L );

			query.execute();
			final Long phoneCount = (Long) query.getOutputParameterValue( 2 );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18302" )
	public void testStoredProcedureNamedParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

			query.setParameter( "personId", 1L );

			query.execute();
			final Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_person_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
			query.setParameter( 1, 1L );

			query.execute();
			final List<Object[]> postComments = query.getResultList();
			assertNotNull( postComments );
		} );
	}

	@Test
	public void testHibernateProcedureCallRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );

			final ProcedureCall call = session.createStoredProcedureCall( "sp_person_phones" );
			final ProcedureParameter<Long> inParam = call.registerParameter(
					1,
					Long.class,
					ParameterMode.IN
			);
			call.setParameter( inParam, 1L );
			call.registerParameter( 2, Class.class, ParameterMode.REF_CURSOR );

			final Output output = call.getOutputs().getCurrent();
			final List<Object[]> postComments = ( (ResultSetOutput) output ).getResultList();
			assertEquals( 2, postComments.size() );
		} );
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Integer phoneCount = (Integer) entityManager
					.createNativeQuery( "SELECT fn_count_phones(:personId) FROM SYSIBM.DUAL" )
					.setParameter( "personId", 1L )
					.getSingleResult();
			assertEquals( 2, phoneCount );
		} );
	}

	@Test
	public void testFunctionCallWithJdbc(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (final PreparedStatement function = connection.prepareStatement(
						"select * from table(fn_person_and_phones( ? ))" )) {
					function.setInt( 1, 1 );
					function.execute();
					try (final ResultSet resultSet = function.getResultSet()) {
						while ( resultSet.next() ) {
							assertThat( resultSet.getLong( 1 ) ).isInstanceOf( Long.class );
							assertThat( resultSet.getString( 2 ) ).isInstanceOf( String.class );
						}
					}
				}
			} );
		} );
	}

	@Test
	public void testSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery function = entityManager.createNamedStoredProcedureQuery( "singleRefCursor" );

			function.execute();

			assertFalse( function.hasMoreResults() );
			Long value = null;

			try (final ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
				while ( resultSet.next() ) {
					value = resultSet.getLong( 1 );
				}
			}
			catch (final SQLException e) {
				fail( e.getMessage() );
			}

			assertEquals( Long.valueOf( 1 ), value );
		} );
	}

	@Test
	public void testOutAndSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery function = entityManager.createNamedStoredProcedureQuery( "outAndRefCursor" );

			function.execute();

			assertFalse( function.hasMoreResults() );
			Long value = null;

			try (final ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
				while ( resultSet.next() ) {
					value = resultSet.getLong( 1 );
				}
			}
			catch (final SQLException e) {
				fail( e.getMessage() );
			}

			assertEquals( value, function.getOutputParameterValue( 2 ) );
		} );
	}

	@Test
	public void testBindParameterAsHibernateType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phone_validity" )
					.registerStoredProcedureParameter( 1, NumericBooleanConverter.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
					.setParameter( 1, true );

			query.execute();
			final List phones = query.getResultList();
			assertEquals( 1, phones.size() );
			assertEquals( "123-456-7890", phones.get( 0 ) );
		} );

		scope.inTransaction( entityManager -> {
			final Vote vote1 = new Vote();
			vote1.setId( 1L );
			vote1.setVoteChoice( true );

			entityManager.persist( vote1 );

			final Vote vote2 = new Vote();
			vote2.setId( 2L );
			vote2.setVoteChoice( false );

			entityManager.persist( vote2 );
		} );

		scope.inTransaction( entityManager -> {
			final StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_votes" )
					.registerStoredProcedureParameter( 1, YesNoConverter.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
					.setParameter( 1, true );

			query.execute();
			final List votes = query.getResultList();
			assertEquals( 1, votes.size() );
			assertEquals( 1, ( (Number) votes.get( 0 ) ).intValue() );
		} );
	}

	@BeforeAll
	public void prepareSchema(EntityManagerFactoryScope scope) {
		scope.inTransaction( (entityManager) -> entityManager.unwrap( Session.class ).doWork( (connection) -> {
			try (final Statement statement = connection.createStatement()) {
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_count_phones(  " +
								"   IN personId INT,  " +
								"   OUT phoneCount INT )  " +
								"BEGIN  " +
								"    SELECT COUNT(*) INTO phoneCount  " +
								"    FROM phone  " +
								"    WHERE person_id = personId; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_person_phones( " +
								"   IN personId INT," +
								"   OUT personPhones CURSOR) " +
								"BEGIN " +
								"    SET personPhones = CURSOR FOR " +
								"    SELECT *" +
								"    FROM phone " +
								"    WHERE person_id = personId; " +
								"    OPEN personPhones; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION fn_count_phones( " +
								"    IN personId INT ) " +
								"    RETURNS INT " +
								"BEGIN " +
								"    DECLARE phoneCount INT; " +
								"    SELECT COUNT(*) INTO phoneCount " +
								"    FROM phone " +
								"    WHERE person_id = personId; " +
								"    RETURN phoneCount; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION fn_person_and_phones(personId INTEGER) " +
								"    RETURNS TABLE " +
								"            ( " +
								"                \"pr.id\"           BIGINT, " +
								"                \"pr.name\"         VARCHAR(255), " +
								"                \"pr.nickName\"     VARCHAR(255), " +
								"                \"pr.address\"      VARCHAR(255), " +
								"                \"pr.createdOn\"    TIMESTAMP, " +
								"                \"pr.version\"      INTEGER, " +
								"                \"ph.id\"           BIGINT, " +
								"                \"ph.person_id\"    BIGINT, " +
								"                \"ph.phone_number\" VARCHAR(255), " +
								"                \"ph.valid\"        BOOLEAN " +
								"            ) " +
								"    BEGIN ATOMIC " +
								"    RETURN SELECT pr.id AS \"pr.id\", " +
								"                   pr.name AS \"pr.name\", " +
								"                   pr.nickName AS \"pr.nickName\", " +
								"                   pr.address AS \"pr.address\", " +
								"                   pr.createdOn AS \"pr.createdOn\", " +
								"                   pr.version AS \"pr.version\", " +
								"                   ph.id AS \"ph.id\", " +
								"                   ph.person_id AS \"ph.person_id\", " +
								"                   ph.phone_number AS \"ph.phone_number\", " +
								"                   ph.valid AS \"ph.valid\" " +
								"            FROM person pr " +
								"                     JOIN phone ph ON pr.id = ph.person_id " +
								"            WHERE pr.id = personId; " +
								"    END; " );
				statement.executeUpdate(
						"CREATE OR REPLACE " +
								"PROCEDURE singleRefCursor(OUT p_recordset CURSOR) " +
								"  BEGIN " +
								"    SET p_recordset = CURSOR FOR " +
								"    SELECT 1 as id " +
								"    FROM sysibm.dual; " +
								"    OPEN p_recordset; " +
								"  END; "
				);
				statement.executeUpdate(
						"CREATE OR REPLACE " +
								"PROCEDURE outAndRefCursor(OUT p_recordset CURSOR, OUT p_value INT) " +
								"  BEGIN " +
								"    SET p_recordset = CURSOR FOR " +
								"    SELECT 1 as id " +
								"    FROM sysibm.dual; " +
								"	 SELECT 1 INTO p_value FROM sysibm.dual; " +
								"    OPEN p_recordset; " +
								"  END; "
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_phone_validity ( " +
								"   IN validity BOOLEAN, " +
								"   OUT personPhones CURSOR ) " +
								"BEGIN " +
								"    SET personPhones = CURSOR FOR " +
								"    SELECT phone_number " +
								"    FROM phone " +
								"    WHERE valid = validity; " +
								"    OPEN personPhones; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_votes ( " +
								"   IN validity BOOLEAN, " +
								"   OUT votes CURSOR ) " +
								"BEGIN " +
								"    SET votes = CURSOR FOR " +
								"    SELECT id " +
								"    FROM vote " +
								"    WHERE vote_choice = validity; " +
								"    OPEN votes; " +
								"END;"
				);
			}
			catch (final SQLException e) {
				System.err.println( "Error exporting procedure and function definitions to DB2 database : " + e.getMessage() );
				e.printStackTrace( System.err );
			}
		} ) );

		scope.inTransaction( (entityManager) -> {
			final Person person1 = new Person( 1L, "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			final Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );
			phone1.setValid( true );

			person1.addPhone( phone1 );

			final Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );
			phone2.setValid( false );

			person1.addPhone( phone2 );
		} );
	}

	@AfterAll
	public void cleanUpSchema(EntityManagerFactoryScope scope) {
		scope.inEntityManager( (em) -> {
			final Session session = em.unwrap( Session.class );
			session.doWork( connection -> {
				try (final Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE sp_count_phones" );
					statement.executeUpdate( "DROP PROCEDURE sp_person_phones" );
					statement.executeUpdate( "DROP FUNCTION fn_count_phones" );
					statement.executeUpdate( "DROP FUNCTION fn_person_and_phones" );
					statement.executeUpdate( "DROP PROCEDURE singleRefCursor" );
					statement.executeUpdate( "DROP PROCEDURE outAndRefCursor" );
					statement.executeUpdate( "DROP PROCEDURE sp_phone_validity" );
					statement.executeUpdate( "DROP PROCEDURE sp_votes" );
				}
				catch (final SQLException ignore) {
				}
			} );

			scope.inTransaction( em, (em2) -> {
				final List<Person> people = em.createQuery( "from Person", Person.class ).getResultList();
				people.forEach( em::remove );
			} );
		} );
	}

	@NamedStoredProcedureQueries( {
			@NamedStoredProcedureQuery(
					name = "singleRefCursor",
					procedureName = "singleRefCursor",
					parameters = {
							@StoredProcedureParameter( mode = ParameterMode.REF_CURSOR, type = void.class )
					}
			),
			@NamedStoredProcedureQuery(
					name = "outAndRefCursor",
					procedureName = "outAndRefCursor",
					parameters = {
							@StoredProcedureParameter( mode = ParameterMode.REF_CURSOR, type = void.class ),
							@StoredProcedureParameter( mode = ParameterMode.OUT, type = Long.class ),
					}
			)
	} )
	@Entity( name = "IdHolder" )
	public static class IdHolder {
		@Id
		Long id;
		String name;
	}
}
