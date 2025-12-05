/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = MySQLDialect.class, matchSubTypes = false, majorVersion = 5)
@RequiresDialect(value = MariaDBDialect.class, majorVersion = 5)
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB doesn't support stored procedures")
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
		}
)
public class MySQLStoredProcedureTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
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

					statement.executeUpdate(
						"CREATE PROCEDURE sp_phones(IN personId INT) " +
						"BEGIN " +
						"    SELECT *  " +
						"    FROM Phone   " +
						"    WHERE person_id = personId;  " +
						"END"
					);

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

					statement.executeUpdate(
						"CREATE PROCEDURE sp_is_null (" +
						"   IN param varchar(255), " +
						"   OUT result tinyint(1) " +
						") " +
						"BEGIN " +
						"    IF (param IS NULL) THEN SET result = true; " +
						"    ELSE SET result = false; " +
						"    END IF; " +
						"END"
					);
				} finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		}
		finally {
			entityManager.close();
		}

		scope.inTransaction(
				em -> {
					Person person1 = new Person( 1L, "John Doe" );
					person1.setNickName( "JD" );
					person1.setAddress( "Earth" );
					person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
																.toInstant( ZoneOffset.UTC ) ) );

					em.persist( person1 );

					Phone phone1 = new Phone( "123-456-7890" );
					phone1.setId( 1L );

					person1.addPhone( phone1 );

					Phone phone2 = new Phone( "098_765-4321" );
					phone2.setId( 2L );

					person1.addPhone( phone2 );
				}
		);
	}

	@AfterEach
	public void destroy(EntityManagerFactoryScope scope) {
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_count_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP FUNCTION IF EXISTS fn_count_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_is_null" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
					query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

					query.setParameter( "personId", 1L );

					query.execute();
					Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
					assertEquals( Long.valueOf( 2 ), phoneCount );
				}
		);
	}

	@Test
	public void testHibernateProcedureCallOutParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );

					ProcedureCall call = session.createStoredProcedureCall( "sp_count_phones" );
					final ProcedureParameter<Long> inParam = call.registerParameter(
							"personId",
							Long.class,
							ParameterMode.IN
					);
					call.registerParameter( "phoneCount", Long.class, ParameterMode.OUT );

					call.setParameter( inParam, 1L );

					Long phoneCount = (Long) call.getOutputs().getOutputParameterValue( "phoneCount" );
					assertEquals( Long.valueOf( 2 ), phoneCount );
				}
		);
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					try {
						StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
						query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
						query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );

						query.setParameter( 2, 1L );

						List<Object[]> personComments = query.getResultList();
						assertEquals( 2, personComments.size() );
					}
					catch (Exception e) {
						assertTrue( Pattern.compile( "Dialect .*? not known to support REF_CURSOR parameters" )
											.matcher( e.getMessage() )
											.matches() );
					}

				}
		);
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
					query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );

					query.setParameter( 1, 1L );

					List<Object[]> personComments = query.getResultList();
					assertEquals( 2, personComments.size() );
				}
		);
	}

	@Test
	public void testHibernateProcedureCallReturnValueParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );

					ProcedureCall call = session.createStoredProcedureCall( "sp_phones" );
					final ProcedureParameter<Long> parameter = call.registerParameter(
							1,
							Long.class,
							ParameterMode.IN
					);

					call.setParameter( parameter, 1L );

					Output output = call.getOutputs().getCurrent();

					List<Object[]> personComments = ( (ResultSetOutput) output ).getResultList();
					assertEquals( 2, personComments.size() );
				}
		);
	}

	@Test
	public void testFunctionWithJDBC(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
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
					assertEquals( Integer.valueOf( 2 ), phoneCount.get() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testStoredProcedureNullParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.OUT );
			procedureCall.setParameter( 1, null );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertTrue( result );
		} );

		scope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.OUT );
			procedureCall.setParameter( 1, "test" );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertFalse( result );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testStoredProcedureNullParameterHibernateWithoutEnablePassingNulls(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.OUT );
			procedureCall.setParameter( 1, null );

			procedureCall.getOutputParameterValue( 2 );

		} );
	}

	@Test
	public void testStoredProcedureNullParameterHibernateWithoutSettingTheParameter(
			EntityManagerFactoryScope
					scope) {

		scope.inTransaction( entityManager -> {
			try {
				ProcedureCall procedureCall = entityManager.unwrap( Session.class ).createStoredProcedureCall(
						"sp_is_null" );
				procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
				procedureCall.registerParameter( 2, Boolean.class, ParameterMode.OUT );

				procedureCall.getOutputParameterValue( 2 );

				fail( "Should have thrown exception" );
			}
			catch (IllegalArgumentException e) {
				assertTrue( e.getMessage().contains( "parameter at position 1" ) );
			}
		} );
	}
}
