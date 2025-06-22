/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.AddressType;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.Partner;
import org.hibernate.testing.orm.domain.userguide.Payment;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.domain.userguide.PhoneType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = MySQLDialect.class, majorVersion = 5)
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB doesn't support stored procedures")
@Jpa(
		annotatedClasses = {
				Person.class,
				Partner.class,
				Phone.class,
				Call.class,
				Payment.class,
				Account.class
		}
)
public class MySQLStoredProcedureTest {

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		destroy( scope );
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					//tag::sql-sp-out-mysql-example[]
					statement.executeUpdate(
							"CREATE PROCEDURE sp_count_phones (" +
									"   IN personId INT, " +
									"   OUT phoneCount INT " +
									") " +
									"BEGIN " +
									"    SELECT COUNT(*) INTO phoneCount " +
									"    FROM Phone p " +
									"    WHERE p.person_id = personId; " +
									"END"
					);
					//end::sql-sp-out-mysql-example[]
					//tag::sql-sp-no-out-mysql-example[]
					statement.executeUpdate(
							"CREATE PROCEDURE sp_phones(IN personId INT) " +
									"BEGIN " +
									"    SELECT *  " +
									"    FROM Phone   " +
									"    WHERE person_id = personId;  " +
									"END"
					);
					//end::sql-sp-no-out-mysql-example[]
					//tag::sql-sp-inout-mysql-example[]
					statement.executeUpdate(
							"CREATE PROCEDURE sp_inout_phones(INOUT phoneId INT) " +
									"BEGIN " +
									"    SELECT MAX(id) INTO phoneId FROM Phone WHERE id > phoneId; " +
									"    SELECT * FROM Phone LIMIT 2;  " +
									"END  "
					);
					//end::sql-sp-inout-mysql-example[]
					//tag::sql-function-mysql-example[]
					statement.executeUpdate(
							"CREATE FUNCTION fn_count_phones(personId integer)  " +
									"RETURNS integer " +
									"DETERMINISTIC " +
									"READS SQL DATA " +
									"BEGIN " +
									"    DECLARE phoneCount integer; " +
									"    SELECT COUNT(*) INTO phoneCount " +
									"    FROM Phone p " +
									"    WHERE p.person_id = personId; " +
									"    RETURN phoneCount; " +
									"END"
					);
					//end::sql-function-mysql-example[]
				}
			} );
		} );
		scope.inTransaction( entityManager -> {
			Person person1 = new Person( "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ) );
			person1.getAddresses().put( AddressType.HOME, "Home address" );
			person1.getAddresses().put( AddressType.OFFICE, "Office address" );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );
			phone1.setType( PhoneType.MOBILE );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );
			phone2.setType( PhoneType.LAND_LINE );

			person1.addPhone( phone2 );
		} );
	}

	@AfterAll
	public void destroy(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_count_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		} );
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		} );
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP FUNCTION IF EXISTS fn_count_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		} );
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_inout_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		} );
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-jpa-call-sp-out-mysql-example[]
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			//end::sql-jpa-call-sp-out-mysql-example[]
			assertEquals( 2l, phoneCount );
		} );
	}

	@Test
	public void testHibernateProcedureCallOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-hibernate-call-sp-out-mysql-example[]
			Session session = entityManager.unwrap( Session.class );

			ProcedureCall call = session.createStoredProcedureCall( "sp_count_phones" );
			ProcedureParameter<Long> parameter = call.registerParameter( "personId", Long.class, ParameterMode.IN );
			call.setParameter( parameter, 1L );
			call.registerParameter( "phoneCount", Long.class, ParameterMode.OUT );

			Long phoneCount = (Long) call.getOutputs().getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
			//end::sql-hibernate-call-sp-out-mysql-example[]
		} );
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		try {
			scope.inTransaction( entityManager -> {
				StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
				query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
				query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );

				query.setParameter( 2, 1L );

				List<Object[]> personComments = query.getResultList();
				assertEquals( 2, personComments.size() );
			} );
		}
		catch (Exception e) {
			assertTrue( Pattern.compile( "Dialect .*? not known to support REF_CURSOR parameters" )
								.matcher( e.getMessage() )
								.matches() );
		}
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-jpa-call-sp-no-out-mysql-example[]
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );

			query.setParameter( 1, 1L );

			List<Object[]> personComments = query.getResultList();
			//end::sql-jpa-call-sp-no-out-mysql-example[]
			assertEquals( 2, personComments.size() );
		} );
	}

	@Test
	public void testHibernateProcedureCallReturnValueParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-hibernate-call-sp-no-out-mysql-example[]
			Session session = entityManager.unwrap( Session.class );

			ProcedureCall call = session.createStoredProcedureCall( "sp_phones" );
			ProcedureParameter<Long> parameter = call.registerParameter( 1, Long.class, ParameterMode.IN );
			call.setParameter( parameter, 1L );

			Output output = call.getOutputs().getCurrent();

			List<Object[]> personComments = ( (ResultSetOutput) output ).getResultList();
			//end::sql-hibernate-call-sp-no-out-mysql-example[]
			assertEquals( 2, personComments.size() );
		} );
	}

	@Test
	public void testFunctionWithJDBC(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-call-function-mysql-example[]
			final AtomicReference<Integer> phoneCount = new AtomicReference<>();
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (CallableStatement function = connection.prepareCall(
						"{ ? = call fn_count_phones(?) }" )) {
					function.registerOutParameter( 1, Types.INTEGER );
					function.setInt( 2, 1 );
					function.execute();
					phoneCount.set( function.getInt( 1 ) );
				}
			} );
			//end::sql-call-function-mysql-example[]
			assertEquals( Integer.valueOf( 2 ), phoneCount.get() );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStoredProcedureInOutParameterAndResultList(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-jpa-call-sp-inout-with-result-list-mysql-example[]
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_inout_phones", Phone.class);
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.INOUT );
			query.setParameter( 1, 1L );
			query.execute();

			Long maxId = (Long) query.getOutputParameterValue(1);
			List supposedToBePhone = query.getResultList();
			assertEquals(2, maxId);
			//end::sql-jpa-call-sp-inout-with-result-list-mysql-example[]
			// now let's see how the JDBC ResultSet is extracted
			// this test should fail as of Hibernate 6.4.1, each item in the result set is an array: [Phone, Long]
			assertInstanceOf(Phone.class, supposedToBePhone.get(0));
		} );
	}
}
