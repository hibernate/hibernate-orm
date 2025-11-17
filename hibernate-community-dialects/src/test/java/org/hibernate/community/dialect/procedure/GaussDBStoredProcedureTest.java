/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.procedure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.orm.test.procedure.Person;
import org.hibernate.orm.test.procedure.Phone;
import org.hibernate.orm.test.procedure.Vote;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.YesNoConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author chen zhida
 *
 * Notes: Original code of this class is based on OracleStoredProcedureTest.
 *
 */
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
				GaussDBStoredProcedureTest.IdHolder.class,
				Vote.class,
				GaussDBStoredProcedureTest.Address.class
		},
		properties = @Setting( name = AvailableSettings.QUERY_PASS_PROCEDURE_PARAMETER_NAMES, value = "true")
)
@RequiresDialect(value = GaussDBDialect.class)
public class GaussDBStoredProcedureTest {

	private Person person1;
	private static final String CITY = "London";
	private static final String STREET = "Lollard Street";
	private static final String ZIP = "SE116UG";

	@Test
	public void testUnRegisteredParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( (em) -> {
			final StoredProcedureQuery function = em.createStoredProcedureQuery( "find_char", Integer.class );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );
			// search-string
			function.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
			// source-string
			function.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );

			function.setParameter( 1, "." );
			function.setParameter( 2, "org.hibernate.query" );

			final Object singleResult = function.getSingleResult();
			Assertions.assertThat( singleResult ).isInstanceOf( Integer.class );
			Assertions.assertThat( singleResult ).isEqualTo( 4 );
		} );
	}

	@Test
	public void testUnRegisteredParameterByName2(EntityManagerFactoryScope scope) {
		scope.inTransaction( (em) -> {
			final StoredProcedureQuery function = em.createStoredProcedureQuery( "find_char", Integer.class );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );
			// search-string
			function.registerStoredProcedureParameter( "search_char", String.class, ParameterMode.IN );
			// source-string
			function.registerStoredProcedureParameter( "string", String.class, ParameterMode.IN );

			function.setParameter( "search_char", "." );
			function.setParameter( "string", "org.hibernate.query" );

			final Object singleResult = function.getSingleResult();
			Assertions.assertThat( singleResult ).isInstanceOf( Integer.class );
			Assertions.assertThat( singleResult ).isEqualTo( 4 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-15542")
	public void testStoredProcedureInAndOutAndRefCursorParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "GET_ADDRESS_BY_NAME" );
					query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "city_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "rec_out", ResultSet.class, ParameterMode.REF_CURSOR );
					query.registerStoredProcedureParameter( "err_out", String.class, ParameterMode.OUT );

					query.setParameter( "street_in", STREET )
							.setParameter( "city_in", CITY );
					query.execute();
					ResultSet rs = (ResultSet) query.getOutputParameterValue( "rec_out" );
					try {
						assertTrue( rs.next() );
						assertThat( rs.getString( "street" ), is( STREET ) );
						assertThat( rs.getString( "city" ), is( CITY ) );
						assertThat( rs.getString( "zip" ), is( ZIP ) );
					}
					catch (SQLException e) {
						throw new RuntimeException( e );
					}
				}
		);
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
					query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.OUT );

					query.setParameter( 1, person1.getId() );

					query.execute();
					Long phoneCount = (Long) query.getOutputParameterValue( 2 );
					assertEquals( Long.valueOf( 2 ), phoneCount );
				}
		);
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_person_phones" );
					query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
					query.setParameter( 1, person1.getId() );

					query.execute();
					List<Object[]> postComments = query.getResultList();
					assertNotNull( postComments );
				}
		);
	}

	@Test
	public void testHibernateProcedureCallRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );

					ProcedureCall call = session.createStoredProcedureCall( "sp_person_phones" );
					final ProcedureParameter<Long> inParam = call.registerParameter(
							1,
							Long.class,
							ParameterMode.IN
					);
					call.setParameter( inParam, person1.getId() );
					call.registerParameter( 2, Class.class, ParameterMode.REF_CURSOR );

					Output output = call.getOutputs().getCurrent();
					List<Object[]> postComments = ( (ResultSetOutput) output ).getResultList();
					assertEquals( 2, postComments.size() );
				}
		);
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					BigDecimal phoneCount = (BigDecimal) entityManager
							.createNativeQuery( "SELECT fn_count_phones(:personId)" )
							.setParameter( "personId", person1.getId() )
							.getSingleResult();
					assertEquals( BigDecimal.valueOf( 2 ), phoneCount );
				}
		);
	}

	@Test
	public void testNamedProcedureCallStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Object[]> postAndComments = entityManager
							.createNamedStoredProcedureQuery( "personAndPhonesFunction" )
							.setParameter( 1, person1.getId() )
							.getResultList();
					Object[] postAndComment = postAndComments.get( 0 );
					Person person = (Person) postAndComment[0];
					Phone phone = (Phone) postAndComment[1];
					assertEquals( 2, postAndComments.size() );
				}
		);
	}

	@Test
	public void testFunctionCallWithJdbc(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );
					session.doWork( connection -> {
						try (CallableStatement function = connection.prepareCall(
								"{ ? = call fn_person_and_phones( ? ) }" )) {
							try {
								function.registerOutParameter( 1, Types.REF_CURSOR );
							}
							catch (SQLException e) {
								function.registerOutParameter( 1, -10 );
							}
							function.setLong( 2, person1.getId() );
							function.execute();
							try (ResultSet resultSet = (ResultSet) function.getObject( 1 );) {
								while ( resultSet.next() ) {
									Long postCommentId = resultSet.getLong( 1 );
									String review = resultSet.getString( 2 );
								}
							}
						}
					} );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery function = entityManager.createNamedStoredProcedureQuery( "singleRefCursor" );

					function.execute();

					assertFalse( function.hasMoreResults() );
					Long value = null;

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
	@JiraKey(value = "HHH-11863")
	public void testOutAndSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery function = entityManager.createNamedStoredProcedureQuery( "outAndRefCursor" );

					function.execute();

					assertFalse( function.hasMoreResults() );
					Long value = null;

					try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
						while ( resultSet.next() ) {
							value = resultSet.getLong( 1 );
						}
					}
					catch (SQLException e) {
						fail( e.getMessage() );
					}

					assertEquals( value, function.getOutputParameterValue( 2 ) );
				} );
	}

	@Test
	@JiraKey(value = "HHH-12661")
	public void testBindParameterAsHibernateType(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phone_validity" )
							.registerStoredProcedureParameter( 1, NumericBooleanConverter.class, ParameterMode.IN )
							.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
							.setParameter( 1, true );

					query.execute();
					List phones = query.getResultList();
					assertEquals( 1, phones.size() );
					assertEquals( "123-456-7890", phones.get( 0 ) );
				} );

		scope.inTransaction(
				entityManager -> {
					Vote vote1 = new Vote();
					vote1.setId( 1L );
					vote1.setVoteChoice( true );

					entityManager.persist( vote1 );

					Vote vote2 = new Vote();
					vote2.setId( 2L );
					vote2.setVoteChoice( false );

					entityManager.persist( vote2 );
				} );

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_votes" )
							.registerStoredProcedureParameter( 1, YesNoConverter.class, ParameterMode.IN )
							.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
							.setParameter( 1, true );

					query.execute();
					List votes = query.getResultList();
					assertEquals( 1, votes.size() );
					assertEquals( 1, ( (Number) votes.get( 0 ) ).intValue() );
				} );
	}


	@BeforeEach
	public void prepareSchema(EntityManagerFactoryScope scope) {
		scope.inTransaction( (entityManager) -> entityManager.unwrap( Session.class ).doWork( (connection) -> {
			try ( Statement statement = connection.createStatement() ) {
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_count_phones (  " +
								"   personId IN BIGINT,  " +
								"   phoneCount OUT BIGINT )  " +
								"AS  " +
								"BEGIN  " +
								"    SELECT COUNT(*) INTO phoneCount  " +
								"    FROM phone  " +
								"    WHERE person_id = personId; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_person_phones ( " +
								"   personId IN NUMBER, " +
								"   personPhones OUT SYS_REFCURSOR ) " +
								"AS  " +
								"BEGIN " +
								"    OPEN personPhones FOR " +
								"    SELECT *" +
								"    FROM phone " +
								"    WHERE person_id = personId; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION fn_count_phones ( " +
								"    personId IN NUMBER ) " +
								"    RETURN NUMBER " +
								"IS " +
								"    phoneCount NUMBER; " +
								"BEGIN " +
								"    SELECT COUNT(*) INTO phoneCount " +
								"    FROM phone " +
								"    WHERE person_id = personId; " +
								"    RETURN( phoneCount ); " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION fn_person_and_phones ( " +
								"    personId IN NUMBER ) " +
								"    RETURN SYS_REFCURSOR " +
								"IS " +
								"    personAndPhones SYS_REFCURSOR; " +
								"BEGIN " +
								"   OPEN personAndPhones FOR " +
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
								"   RETURN personAndPhones; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE " +
								"PROCEDURE singleRefCursor(p_recordset OUT SYS_REFCURSOR) AS " +
								"  BEGIN " +
								"    OPEN p_recordset FOR " +
								"    SELECT 1 as id; " +
								"  END; "
				);
				statement.executeUpdate(
						"CREATE OR REPLACE " +
								"PROCEDURE outAndRefCursor(p_recordset OUT SYS_REFCURSOR, p_value OUT BIGINT) AS " +
								"  BEGIN " +
								"    OPEN p_recordset FOR " +
								"    SELECT 1 as id; " +
								"	 SELECT 1 INTO p_value; " +
								"  END; "
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_phone_validity ( " +
								"   validity IN NUMBER, " +
								"   personPhones OUT SYS_REFCURSOR ) " +
								"AS  " +
								"BEGIN " +
								"    OPEN personPhones FOR " +
								"    SELECT phone_number " +
								"    FROM phone " +
								"    WHERE valid = validity; " +
								"END;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_votes ( " +
								"   validity IN CHAR, " +
								"   votes OUT SYS_REFCURSOR ) " +
								"AS  " +
								"BEGIN " +
								"    OPEN votes FOR " +
								"    SELECT id " +
								"    FROM vote " +
								"    WHERE vote_choice = validity; " +
								"END;"
				);

				statement.execute(
						"CREATE OR REPLACE FUNCTION find_char(" +
								"		search_char IN CHAR, " +
								"		string IN VARCHAR," +
								"		start_idx IN NUMBER DEFAULT 1) " +
								"RETURN NUMBER " +
								"IS " +
								"		pos NUMBER; " +
								"BEGIN " +
								"    SELECT INSTR( string, search_char, start_idx ) INTO pos; " +
								"    RETURN pos; " +
								"END;"
				);

				statement.execute(
						"CREATE OR REPLACE PROCEDURE GET_ADDRESS_BY_NAME (" +
								" street_in IN ADDRESS_TABLE.STREET%TYPE," +
								" city_in IN ADDRESS_TABLE.CITY%TYPE," +
								" rec_out OUT SYS_REFCURSOR," +
								" err_out OUT VARCHAR)" +
								" AS" +
								" BEGIN" +
								" OPEN rec_out FOR" +
								" SELECT A.STREET, A.CITY, A.zip" +
								" FROM  ADDRESS_TABLE A " +
								" WHERE " +
								" A.STREET = street_in" +
								" AND A.CITY = city_in;" +
								" EXCEPTION " +
								" WHEN OTHERS THEN " +
								" err_out := SQLCODE || ' ' || SQLERRM;" +
								" END;" );
			}
			catch (SQLException e) {
				e.printStackTrace(System.err);
			}
		} ) );

		scope.inTransaction( (entityManager) -> {
			person1 = new Person( 1L, "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
					.toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );
			phone1.setValid( true );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );
			phone2.setValid( false );

			person1.addPhone( phone2 );

			Address address = new Address( 1l, STREET, CITY, ZIP );
			entityManager.persist( address );
		} );
	}

	@AfterEach
	public void cleanUpSchema(EntityManagerFactoryScope scope) {
		scope.inEntityManager( (em) -> {
			final Session session = em.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE sp_count_phones" );
					statement.executeUpdate( "DROP PROCEDURE sp_person_phones" );
					statement.executeUpdate( "DROP PROCEDURE singleRefCursor" );
					statement.executeUpdate( "DROP PROCEDURE outAndRefCursor" );
					statement.executeUpdate( "DROP PROCEDURE sp_phone_validity" );
					statement.executeUpdate( "DROP PROCEDURE sp_votes" );
					statement.executeUpdate( "DROP FUNCTION fn_count_phones" );
					statement.executeUpdate( "DROP FUNCTION fn_person_and_phones" );
					statement.executeUpdate( "DROP FUNCTION find_char" );
				}
				catch (SQLException ignore) {
				}
			} );

			scope.inTransaction( em, (em2) -> {
				final List<Person> people = em.createQuery( "from Person", Person.class ).getResultList();
				people.forEach( em::remove );

				em.createQuery( "delete IdHolder" ).executeUpdate();
				em.createQuery( "delete Address" ).executeUpdate();
			});
		} );
	}

	@NamedStoredProcedureQueries({
			@NamedStoredProcedureQuery(
					name = "singleRefCursor",
					procedureName = "singleRefCursor",
					parameters = {
							@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class)
					}
			),
			@NamedStoredProcedureQuery(
					name = "outAndRefCursor",
					procedureName = "outAndRefCursor",
					parameters = {
							@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class),
							@StoredProcedureParameter(mode = ParameterMode.OUT, type = Long.class),
					}
			)
	})
	@Entity(name = "IdHolder")
	public static class IdHolder {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "Address")
	@Table(name="ADDRESS_TABLE")
	public static class Address{
		@Id
		@Column(name="ID")
		private long id;
		@Column(name="STREET")
		private String street;
		@Column(name="CITY")
		private String city;
		@Column(name="ZIP")
		private String zip;

		public Address() {
		}

		public Address(long id, String street, String city, String zip) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.zip = zip;
		}

		public long getId() {
			return id;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}

		public String getZip() {
			return zip;
		}
	}
}
