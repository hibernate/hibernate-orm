/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.jpa.HibernateHints;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SybaseASEDialect.class)
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
		},
		properties = @Setting( name = AvailableSettings.QUERY_PASS_PROCEDURE_PARAMETER_NAMES, value = "true")
)
public class SybaseStoredProcedureTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		// Force creation of the tables because Sybase checks that dependent tables exist
		scope.getEntityManagerFactory();
		doInAutoCommit(
				"DROP PROCEDURE sp_count_phones",
				"DROP FUNCTION fn_count_phones",
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
						"    DECLARE @phoneCount int  " +
						"    SELECT @phoneCount = COUNT(*) " +
						"    FROM Phone   " +
						"    WHERE person_id = @personId  " +
						"    RETURN @phoneCount  " +
						"END",
				"sp_procxmode sp_count_phones, 'chained'"
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
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.IsJtds.class, comment = "only JTDS supports named parameters and if named parameter are not supported then the parameter should be registered in the same order as defined in the stored procedure")
	public void testStoredProcedureOutParameterDifferentParametersRegistrationOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.IsJtds.class, reverse = true, comment = "jTDS can't handle calling functions")
	public void testFunctionReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "hibernate_orm_test.fn_count_phones", Long.class );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getSingleResult();
			Assert.assertEquals( Long.valueOf( 2 ), phoneCount );
		});
	}
}
