/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.dialect.OracleDialect;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = OracleDialect.class)
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
public class OracleStoredProcedureTest {

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			session.doWork(connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_count_phones ( " +
						"   personId IN NUMBER,  " +
						"   phoneCount OUT NUMBER)  " +
						"AS  " +
						"BEGIN  " +
						"    SELECT COUNT(*) INTO phoneCount  " +
						"    FROM phone  " +
						"    WHERE person_id = personId; " +
						"END;"
					);
					//tag::sql-sp-ref-cursor-oracle-example[]
					statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_person_phones (" +
						"   personId IN NUMBER, " +
						"   personPhones OUT SYS_REFCURSOR) " +
						"AS  " +
						"BEGIN " +
						"    OPEN personPhones FOR " +
						"    SELECT *" +
						"    FROM phone " +
						"    WHERE person_id = personId; " +
						"END;"
					);
					//end::sql-sp-ref-cursor-oracle-example[]
					statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION fn_count_phones (" +
						"    personId IN NUMBER) " +
						"    RETURN NUMBER " +
						"IS " +
						"    phoneCount NUMBER; " +
						"BEGIN " +
						"    SELECT COUNT(*) INTO phoneCount " +
						"    FROM phone " +
						"    WHERE person_id = personId; " +
						"    RETURN(phoneCount); " +
						"END;"
					);
				}
			});
		});
		scope.inTransaction( entityManager -> {
			Person person1 = new Person("John Doe");
			person1.setNickName("JD");
			person1.setAddress("Earth");
			person1.setCreatedOn(LocalDateTime.of(2000, 1, 1, 0, 0, 0)) ;
			person1.getAddresses().put(AddressType.HOME, "Home address");
			person1.getAddresses().put(AddressType.OFFICE, "Office address");

			entityManager.persist(person1);

			Phone phone1 = new Phone("123-456-7890");
			phone1.setId(1L);
			phone1.setType(PhoneType.MOBILE);

			person1.addPhone(phone1);

			Phone phone2 = new Phone("098_765-4321");
			phone2.setId(2L);
			phone2.setType(PhoneType.LAND_LINE);

			person1.addPhone(phone2);
		});
	}

	@AfterAll
	public void destroy(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			session.doWork(connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate("DROP PROCEDURE sp_count_phones");
				}
				catch (SQLException ignore) {
				}
			});
		});
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			session.doWork(connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate("DROP PROCEDURE sp_person_phones");
				}
				catch (SQLException ignore) {
				}
			});
		});
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			session.doWork(connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate("DROP FUNCTION fn_count_phones");
				}
				catch (SQLException ignore) {
				}
			});
		});
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_count_phones");
			query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);

			query.setParameter(1, 1L);

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue(2);
			assertEquals(2l, phoneCount);
		});
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-jpa-call-sp-ref-cursor-oracle-example[]
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_person_phones");
			query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter(2, Class.class, ParameterMode.REF_CURSOR);
			query.setParameter(1, 1L);

			query.execute();
			List<Object[]> postComments = query.getResultList();
			//end::sql-jpa-call-sp-ref-cursor-oracle-example[]
			assertNotNull(postComments);
		});
	}

	@Test
	public void testStoredProcedureRefCursorUsingNamedQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-jpa-call-sp-ref-cursor-oracle-named-query-example[]
			List<Object[]> postComments = entityManager
			.createNamedStoredProcedureQuery("sp_person_phones")
			.setParameter("personId", 1L)
			.getResultList();
			//end::sql-jpa-call-sp-ref-cursor-oracle-named-query-example[]

			assertNotNull(postComments);
		});
	}

	@Test
	public void testHibernateProcedureCallRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::sql-hibernate-call-sp-ref-cursor-oracle-example[]
			Session session = entityManager.unwrap(Session.class);

			ProcedureCall call = session.createStoredProcedureCall("sp_person_phones");
			ProcedureParameter<Long> parameter = call.registerParameter(1, Long.class, ParameterMode.IN);
			call.setParameter(parameter, 1L);
			call.registerParameter(2, Class.class, ParameterMode.REF_CURSOR);

			Output output = call.getOutputs().getCurrent();
			List<Object[]> postComments = ((ResultSetOutput) output).getResultList();
			assertEquals(2, postComments.size());
			//end::sql-hibernate-call-sp-ref-cursor-oracle-example[]
		});
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			BigDecimal phoneCount = (BigDecimal) entityManager
					.createNativeQuery( "SELECT fn_count_phones(:personId) FROM DUAL" )
					.setParameter( "personId", 1 )
					.getSingleResult();
			assertEquals( BigDecimal.valueOf( 2L ), phoneCount );
		} );
	}
}
