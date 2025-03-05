/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
				SQLServerStoredProcedureTest.Address.class
		},
		properties = @Setting( name = AvailableSettings.QUERY_PASS_PROCEDURE_PARAMETER_NAMES, value = "true")
)
public class SQLServerStoredProcedureTest {

	private static final String CITY = "London";
	private static final String STREET = "Lollard Street";
	private static final String ZIP = "SE116UG";

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		doInAutoCommit(
				"DROP PROCEDURE sp_count_phones",
				"DROP FUNCTION fn_count_phones",
				"DROP PROCEDURE sp_phones",
				"DROP PROCEDURE sp_zip_by_city_and_street",
				"DROP PROCEDURE sp_insert_address",
				"CREATE PROCEDURE sp_count_phones " +
						"   @personId INT, " +
						"   @phoneCount INT OUTPUT " +
						"AS " +
						"BEGIN " +
						"   SELECT @phoneCount = COUNT(*)  " +
						"   FROM Phone  " +
						"   WHERE person_id = @personId " +
						"END",
				"CREATE FUNCTION fn_count_phones (@personId INT)  " +
						"RETURNS INT  " +
						"AS  " +
						"BEGIN  " +
						"    DECLARE @phoneCount int;  " +
						"    SELECT @phoneCount = COUNT(*) " +
						"    FROM Phone   " +
						"    WHERE person_id = @personId;  " +
						"    RETURN(@phoneCount);  " +
						"END",
				"CREATE PROCEDURE sp_phones " +
						"    @personId INT, " +
						"    @phones CURSOR VARYING OUTPUT " +
						"AS " +
						"    SET NOCOUNT ON; " +
						"    SET @phones = CURSOR " +
						"    FORWARD_ONLY STATIC FOR " +
						"        SELECT *  " +
						"        FROM Phone   " +
						"        WHERE person_id = @personId;  " +
						"    OPEN @phones;" +
						"END",
				"CREATE PROCEDURE sp_insert_address " +
						"	@id BIGINT," +
						"	@street VARCHAR(255) = '" + STREET + "'," +
						"	@zip VARCHAR(255)," +
						"	@city VARCHAR(255) =  '" + CITY + "'" +
						"AS " +
						"BEGIN " +
						" 	INSERT INTO " +
						" 	 ADDRESS_TABLE (ID, STREET, CITY, ZIP) " +
						" 	VALUES ( " +
						" 	@id," +
						" 	@street," +
						" 	@city," +
						" 	@zip);" +
						"END",
				"CREATE PROCEDURE sp_zip_by_city_and_street " +
						"	@street_in VARCHAR(255)," +
						"	@city_in VARCHAR(255) =  '" + CITY + "'," +
						"	@zip_out VARCHAR(255) OUTPUT " +
						"AS " +
						"BEGIN " +
						" 	SELECT @zip_out = A.ZIP" +
						" 	FROM  ADDRESS_TABLE A " +
						" 	WHERE " +
						" 	A.STREET = @street_in" +
						" 	AND A.CITY = @city_in;" +
						"END"
		);

		scope.inTransaction( entityManager -> {
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
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
	public void testStoredProcedureDefaultValue(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_insert_address" );
					query.registerStoredProcedureParameter( "street", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "id", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "zip", String.class, ParameterMode.IN );

					query.setParameter( "id", 2 )
							.setParameter( "street", STREET )
							.setParameter( "zip", "SE116UG" );
					query.execute();
				}
		);
	}


	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			try {
				StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
				query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
				query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
				query.setParameter( 1, 1L );

				query.execute();
				List<Object[]> postComments = query.getResultList();
				assertNotNull( postComments );
			}
			catch (Exception e) {
				assertTrue( Pattern.compile( "Dialect .*? not known to support REF_CURSOR parameters" )
									.matcher( e.getMessage() )
									.matches() );
			}
		} );
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				CallableStatement function = null;
				try {
					function = connection.prepareCall( "{ ? = call fn_count_phones(?) }" );
					function.registerOutParameter( 1, Types.INTEGER );
					function.setInt( 2, 1 );
					function.execute();
					int phoneCount = function.getInt( 1 );
					assertEquals( 2, phoneCount );
				}
				finally {
					if ( function != null ) {
						function.close();
					}
				}
			} );
		} );
	}


	@Test
	@JiraKey(value = "HHH-18280")
	public void testStoredProcedureInAndOutParametersInvertedParamRegistationOrder2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_zip_by_city_and_street" );
					query.registerStoredProcedureParameter( "city_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "zip_out", String.class, ParameterMode.OUT );

					query.setParameter( "street_in", STREET )
							.setParameter( "city_in", CITY );
					query.execute();
					assertThat( (String) query.getOutputParameterValue( "zip_out" ) ).isEqualTo( ZIP );

				}
		);
	}


	@Test
	@JiraKey(value = "HHH-18280")
	public void testStoredProcedureInAndOutParametersInvertedParamRegistationOrder2_(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );
					session.doWork( connection -> {
						try (CallableStatement statement = connection.prepareCall(
								"{call sp_zip_by_city_and_street ( @zip_out = ?, @city_in = ?,  @street_in = ?   )}" )) {
							statement.registerOutParameter( 1, Types.VARCHAR );
							statement.setString( 2, CITY );
							statement.setString( 3, STREET );
							statement.execute();
							assertThat( (String) statement.getString( 1 ) ).isEqualTo( ZIP );

						}
					} );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-18280")
	public void testStoredProcedureInAndOutParametersInvertedParamRegistationOrder3(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_zip_by_city_and_street" );
					query.registerStoredProcedureParameter( "zip_out", String.class, ParameterMode.OUT );
					query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "city_in", String.class, ParameterMode.IN );

					query.setParameter( "city_in", CITY )
							.setParameter( "street_in", STREET );
					query.execute();
					assertThat( (String) query.getOutputParameterValue( "zip_out" ) ).isEqualTo( ZIP );

				}
		);
	}

	@Test
	public void testUnRegisteredParameterByName(EntityManagerFactoryScope scope) {
		scope.inTransaction( (entityManager) -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_zip_by_city_and_street" );
			query.registerStoredProcedureParameter( "street_in", String.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "zip_out", String.class, ParameterMode.OUT );
			query.setParameter( "street_in", STREET );
			query.execute();

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
