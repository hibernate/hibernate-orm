/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;

import org.hibernate.dialect.HANADialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea, Jonathan Bregler
 */
@RequiresDialect(HANADialect.class)
@DomainModel(
		annotatedClasses = {
				Person.class,
				Phone.class,
				HANAStoredProcedureTest.IdHolder.class,
				Vote.class
		}
)
@SessionFactory
public class HANAStoredProcedureTest {

	@NamedStoredProcedureQueries({
			@NamedStoredProcedureQuery(name = "singleRefCursor", procedureName = "singleRefCursor", parameters = {
					@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class)
			}),
			@NamedStoredProcedureQuery(name = "outAndRefCursor", procedureName = "outAndRefCursor", parameters = {
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class)
			})
	})
	@Entity(name = "IdHolder")
	public static class IdHolder {
		@Id
		Long id;
		String name;
	}

	@BeforeEach
	public void prepareSchemaAndData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.doWork( connection -> {
				try ( Statement statement = connection.createStatement() ) {
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_count_phones (  " +
									"   IN personId INTEGER,  " +
									"   OUT phoneCount INTEGER )  " +
									"AS  " +
									"BEGIN  " +
									"    SELECT COUNT(*) INTO phoneCount  " +
									"    FROM phone  " +
									"    WHERE person_id = :personId; " +
									"END;" );
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_person_phones ( " +
									"   IN personId INTEGER, " +
									"   OUT personPhones phone ) " +
									"AS  " +
									"BEGIN " +
									"    personPhones = " +
									"    SELECT *" +
									"    FROM phone " +
									"    WHERE person_id = :personId; " +
									"END;" );
					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION fn_count_phones ( " +
									"    IN personId INTEGER ) " +
									"    RETURNS phoneCount INTEGER " +
									"AS " +
									"BEGIN " +
									"    SELECT COUNT(*) INTO phoneCount " +
									"    FROM phone " +
									"    WHERE person_id = :personId; " +
									"END;" );
					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION fn_person_and_phones ( " +
									"    IN personId INTEGER ) " +
									"    RETURNS TABLE (\"pr.id\" BIGINT,"
									+ "                 \"pr.name\" NVARCHAR(5000),"
									+ "                 \"pr.nickName\" NVARCHAR(5000),"
									+ "                 \"pr.address\" NVARCHAR(5000),"
									+ "                 \"pr.createdOn\" TIMESTAMP,"
									+ "                 \"pr.version\" INTEGER,"
									+ "                 \"ph.id\" BIGINT,"
									+ "                 \"ph.person_id\" BIGINT,"
									+ "                 \"ph.phone_number\" NVARCHAR(5000),"
									+ "                 \"ph.valid\" BOOLEAN)" +
									"AS " +
									"BEGIN " +
									"   RETURN " +
									"        SELECT " +
									"            pr.id AS \"pr.id\", " +
									"            pr.name AS \"pr.name\", " +
									"            pr.nickName AS \"pr.nickName\", " +
									"            pr.address AS \"pr.address\", " +
									"            pr.createdOn AS \"pr.createdOn\", " +
									"            pr.version AS \"pr.version\", " +
									"            ph.id AS \"ph.id\", " +
									"            ph.person_id AS \"ph.person_id\", " +
									"            ph.phone_number AS \"ph.phone_number\", " +
									"            ph.valid AS \"ph.valid\" " +
									"       FROM person pr " +
									"       JOIN phone ph ON pr.id = ph.person_id " +
									"       WHERE pr.id = personId; " +
									"END;" );
					statement.executeUpdate(
							"CREATE OR REPLACE " +
									"PROCEDURE singleRefCursor(OUT p_recordset TABLE(id INTEGER)) AS " +
									"  BEGIN " +
									"    p_recordset = " +
									"    SELECT 1 as id " +
									"    FROM SYS.DUMMY; " +
									"  END; " );
					statement.executeUpdate(
							"CREATE OR REPLACE " +
									"PROCEDURE outAndRefCursor(OUT p_value INTEGER, OUT p_recordset TABLE(id INTEGER)) AS " +
									"  BEGIN " +
									"    p_recordset = " +
									"    SELECT 1 as id " +
									"    FROM SYS.DUMMY; " +
									"	 SELECT 1 INTO p_value FROM SYS.DUMMY; " +
									"  END; " );
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_phone_validity ( " +
									"   IN validity BOOLEAN, " +
									"   OUT personPhones TABLE (\"phone_number\" VARCHAR(255)) ) " +
									"AS  " +
									"BEGIN " +
									"    personPhones = SELECT phone_number as \"phone_number\" " +
									"    FROM phone " +
									"    WHERE valid = validity; " +
									"END;" );
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_votes ( " +
									"   IN validity VARCHAR(1), " +
									"   OUT votes TABLE (\"id\" BIGINT) ) " +
									"AS  " +
									"BEGIN " +
									"    votes = SELECT id as \"id\" " +
									"    FROM vote " +
									"    WHERE vote_choice = validity; " +
									"END;" );
				}
			} );
		} );

		scope.inTransaction( (session) -> {
			Person person1 = new Person( 1L, "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ) );

			session.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );
			phone1.setValid( true );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );
			phone2.setValid( false );

			person1.addPhone( phone2 );
		} );
	}

	@AfterEach
	public void cleanUpSchemaAndData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Phone" ).executeUpdate();
			session.createMutationQuery( "delete Person" ).executeUpdate();
		} );

		scope.inTransaction( (session) -> session.doWork( connection -> {
			try ( Statement statement = connection.createStatement() ) {
				statement.executeUpdate( "DROP PROCEDURE sp_count_phones" );
				statement.executeUpdate( "DROP PROCEDURE sp_person_phones" );
				statement.executeUpdate( "DROP FUNCTION fn_count_phones" );
				statement.executeUpdate( "DROP PROCEDURE singleRefCursor" );
				statement.executeUpdate( "DROP PROCEDURE outAndRefCursor" );

			}
			catch (SQLException ignore) {
			}
		} ) );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testStoredProcedureOutParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			StoredProcedureQuery query = session.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.OUT );

			query.setParameter( 1, 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( 2 );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testStoredProcedureRefCursor(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			StoredProcedureQuery query = session.createStoredProcedureQuery( "sp_person_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
			query.setParameter( 1, 1L );

			query.execute();
			//noinspection unchecked
			List<Object[]> postComments = query.getResultList();
			Assertions.assertNotNull( postComments );
		} );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testHibernateProcedureCallRefCursor(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			ProcedureCall call = session.createStoredProcedureCall( "sp_person_phones" );
			final ProcedureParameter<Long> inParam = call.registerParameter(
					1,
					Long.class,
					ParameterMode.IN
			);
			call.setParameter( inParam, 1L );

			call.registerParameter( 2, Class.class, ParameterMode.REF_CURSOR );

			Output output = call.getOutputs().getCurrent();
			//noinspection unchecked
			List<Object[]> postComments = ( (ResultSetOutput) output ).getResultList();
			assertEquals( 2, postComments.size() );
		} );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testStoredProcedureReturnValue(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Integer phoneCount = (Integer) session
					.createNativeQuery( "SELECT fn_count_phones(:personId) FROM SYS.DUMMY", Integer.class )
					.setParameter( "personId", 1 )
					.getSingleResult();
			assertEquals( Integer.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testNamedNativeQueryStoredProcedureRefCursor(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			List<Object[]> postAndComments = session.createNamedQuery( "fn_person_and_phones_hana", Object[].class )
					.setParameter( 1, 1L )
					.getResultList();
			assertThat(  postAndComments ).hasSize( 2 );
			Object[] postAndComment = postAndComments.get( 0 );
			assertThat( postAndComment[0] ).isInstanceOf( Person.class );
			assertThat( postAndComment[1] ).isInstanceOf( Phone.class );
		} );

	}

	@Test
	@JiraKey( "HHH-12138")
	public void testFunctionCallWithJDBC(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.doWork( connection -> {
			try ( PreparedStatement function = connection.prepareStatement( "select * from fn_person_and_phones( ? )" ) ) {
				function.setInt( 1, 1 );
				function.execute();
				try ( ResultSet resultSet = function.getResultSet() ) {
					while ( resultSet.next() ) {
						assertThat( resultSet.getLong( 1 ) ).isInstanceOf( Long.class );
						assertThat( resultSet.getString( 2 ) ).isInstanceOf( String.class );
					}
				}
			}
		} ) );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testSysRefCursorAsOutParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			StoredProcedureQuery function = session.createNamedStoredProcedureQuery( "singleRefCursor" );
			function.execute();

			Integer value = (Integer) function.getSingleResult();
			Assertions.assertFalse( function.hasMoreResults() );
			assertEquals( Integer.valueOf( 1 ), value );
		} );
	}

	@Test
	@JiraKey( "HHH-12138")
	public void testOutAndSysRefCursorAsOutParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			StoredProcedureQuery function = session.createNamedStoredProcedureQuery( "outAndRefCursor" );
			function.execute();

			Integer value = (Integer) function.getSingleResult();
			assertEquals( Integer.valueOf( 1 ), value );
			assertEquals( 1, function.getOutputParameterValue( 1 ) );
			Assertions.assertFalse( function.hasMoreResults() );
		} );
	}

	@Test
	@JiraKey( "HHH-12661")
	public void testBindParameterAsHibernateType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			StoredProcedureQuery query = session.createStoredProcedureQuery( "sp_phone_validity" )
					.registerStoredProcedureParameter( 1, NumericBooleanConverter.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
					.setParameter( 1, true );

			query.execute();
			List<?> phones = query.getResultList();
			assertEquals( 1, phones.size() );
			assertEquals( "123-456-7890", phones.get( 0 ) );
		} );

		scope.inTransaction( (session) -> {
			Vote vote1 = new Vote();
			vote1.setId( 1L );
			vote1.setVoteChoice( true );

			session.persist( vote1 );

			Vote vote2 = new Vote();
			vote2.setId( 2L );
			vote2.setVoteChoice( false );

			session.persist( vote2 );
		} );

		scope.inTransaction( (session) -> {
			StoredProcedureQuery query = session.createStoredProcedureQuery( "sp_votes" )
					.registerStoredProcedureParameter( 1, YesNoConverter.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
					.setParameter( 1, true );

			query.execute();
			List<?> votes = query.getResultList();
			assertEquals( 1, votes.size() );
			assertEquals( 1, ( (Number) votes.get( 0 ) ).intValue() );
		} );
	}
}
