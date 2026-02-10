/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
				PostgreSQLStoredProcedureTest.Address.class
		},
		properties = @Setting( name = AvailableSettings.QUERY_PASS_PROCEDURE_PARAMETER_NAMES, value = "true")
)
@RequiresDialect(value = PostgreSQLDialect.class)
public class PostgreSQLStoredProcedureTest {

	private static final String CITY = "London";
	private static final String STREET = "Lollard Street";
	private static final String ZIP = "SE116UG";

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.INOUT );

			query.setParameter( "personId", 1L );
			query.setParameter( "phoneCount", null );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14, comment = "Stored procedure OUT parameters are only supported since version 14")
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, void.class, ParameterMode.REF_CURSOR );

			query.setParameter( 1, 1L );

			List<Object[]> phones = query.getResultList();
			assertEquals( 2, phones.size() );
		} );
	}

	@Test
	public void testStoredProcedureWithJDBC(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long phoneCount = session.doReturningWork( connection -> {
				CallableStatement procedure = null;
				try {
					procedure = connection.prepareCall( "{ call sp_count_phones(?,?) }" );
					procedure.registerOutParameter( 2, Types.BIGINT );
					procedure.setLong( 1, 1L );
					procedure.setNull( 2, Types.BIGINT );
					procedure.execute();
					return procedure.getLong( 2 );
				}
				finally {
					if ( procedure != null ) {
						procedure.close();
					}
				}
			} );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@JiraKey("HHH-11863")
	@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14, comment = "Stored procedure OUT parameters are only supported since version 14")
	public void testSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Long value = null;

			Session session = entityManager.unwrap( Session.class );

			try (ResultSet resultSet = session.doReturningWork( connection -> {
				CallableStatement procedure = null;
				try {
					procedure = connection.prepareCall( "call singleRefCursor(?)" );
					procedure.registerOutParameter( 1, Types.REF_CURSOR );
					procedure.execute();
					return (ResultSet) procedure.getObject( 1 );
				}
				finally {
					if ( procedure != null ) {
						procedure.close();
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


			StoredProcedureQuery procedure = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			procedure.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );

			procedure.execute();

			assertFalse( procedure.hasMoreResults() );

			value = null;
			try (ResultSet resultSet = (ResultSet) procedure.getOutputParameterValue( 1 )) {
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
	@JiraKey("HHH-12905")
	public void testStoredProcedureNullParameterHibernate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.INOUT );
			procedureCall.setParameter( 1, null );
			procedureCall.setParameter( 2, null );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertTrue( result );
		} );

		scope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.INOUT );
			procedureCall.setParameter( 1, "test" );
			procedureCall.setParameter( 2, null );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertFalse( result );
		} );
	}

	@Test
	@JiraKey("HHH-12905")
	public void testStoredProcedureNullParameterHibernateWithoutEnablePassingNulls(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( "result", Boolean.class, ParameterMode.INOUT );
			procedureCall.setParameter( "param", null );
			procedureCall.setParameter( "result", null );

			procedureCall.getOutputParameterValue( "result" );
		} );
	}

	@Test
	public void testStoredProcedureNullParameterHibernateWithoutSettingTheParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			try {
				ProcedureCall procedureCall = entityManager.unwrap( Session.class )
						.createStoredProcedureCall( "sp_is_null" );
				procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
				procedureCall.registerParameter( "result", Boolean.class, ParameterMode.OUT );

				procedureCall.execute();

				fail( "Should have thrown exception" );
			}
			catch (IllegalArgumentException e) {
				assertTrue( e.getMessage().contains( "parameter named 'param'" ) );
			}
		} );
	}

	@Test
	@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14, comment = "Stored procedure OUT parameters are only supported since version 14")
	public void testStoredProcedureInAndOutAndRefCursorParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_get_address_by_street_city" );
					query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "city_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "rec_out", ResultSet.class, ParameterMode.REF_CURSOR );

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
	@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14, comment = "Stored procedure OUT parameters are only supported since version 14")
	public void testStoredProcedureInAndOutAndRefCursorParametersDifferentRegistrationOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_get_address_by_street_city" );
					query.registerStoredProcedureParameter( "city_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "rec_out", ResultSet.class, ParameterMode.REF_CURSOR );

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
	@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14, comment = "Stored procedure OUT parameters are only supported since version 14")
	public void testStoredProcedureInAndOutAndRefCursorParametersDifferentRegistrationOrder2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_get_address_by_street_city" );
					query.registerStoredProcedureParameter( "rec_out", ResultSet.class, ParameterMode.REF_CURSOR );
					query.registerStoredProcedureParameter( "city_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );

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

	@BeforeEach
	public void prepareSchema(EntityManagerFactoryScope scope) {
		scope.inTransaction( (entityManager) -> entityManager.unwrap( Session.class ).doWork( (connection) -> {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_count_phones( " +
								"   IN personId bigint, " +
								"   INOUT phoneCount bigint) " +
								"   AS " +
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
						"CREATE OR REPLACE PROCEDURE sp_phones(IN personId BIGINT, INOUT phones REFCURSOR) " +
								"    AS " +
								"$BODY$ " +
								"    BEGIN " +
								"        OPEN phones FOR  " +
								"            SELECT *  " +
								"            FROM phone   " +
								"            WHERE person_id = personId;  " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE singleRefCursor(INOUT p_recordset REFCURSOR) " +
								"   AS " +
								"$BODY$ " +
								"    BEGIN " +
								"      OPEN p_recordset FOR SELECT 1; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_is_null( " +
								"   IN param varchar(255), " +
								"   INOUT result boolean) " +
								"   AS " +
								"$BODY$ " +
								"    BEGIN " +
								"    select param is null into result; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;"
				);
				statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_get_address_by_street_city (" +
								" IN street_in  varchar(255) ," +
								" IN city_in varchar(255)" +
								" ,INOUT rec_out REFCURSOR" +
								" )" +
								" AS" +
								" $BODY$ " +
								" BEGIN" +
								" OPEN rec_out FOR" +
								" SELECT * " +
								" FROM  ADDRESS_TABLE A " +
								" WHERE " +
								" A.STREET = street_in" +
								" AND A.CITY = city_in;" +
								" END; " +
								" $BODY$ " +
								" LANGUAGE plpgsql"
				);
			}
			catch (SQLException e) {
				System.err.println( "Error exporting procedure and function definitions to Oracle database : " + e.getMessage() );
				e.printStackTrace( System.err );
			}
		} ) );

		scope.inTransaction( (entityManager) -> {
			Person person1 = new Person( 1L, "John Doe" );
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
					statement.executeUpdate( "DROP PROCEDURE sp_count_phones(bigint, bigint)" );
					statement.executeUpdate( "DROP PROCEDURE sp_phones(bigint, refcursor)" );
					statement.executeUpdate( "DROP PROCEDURE singleRefCursor(refcursor)" );
					statement.executeUpdate( "DROP PROCEDURE sp_is_null(varchar, boolean)" );
					statement.executeUpdate( "DROP PROCEDURE sp_get_address_by_street_city(varchar,varchar,refcursor)" );
				}
				catch (SQLException ignore) {
				}
			} );

			scope.inTransaction( em, (em2) -> {
				final List<Person> people = em.createQuery( "from Person", Person.class ).getResultList();
				people.forEach( em::remove );

				em.createQuery( "delete Address" ).executeUpdate();
			} );
		} );
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {
		@Id
		@Column(name = "ID")
		private long id;
		@Column(name = "STREET")
		private String street;
		@Column(name = "CITY")
		private String city;
		@Column(name = "ZIP")
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
