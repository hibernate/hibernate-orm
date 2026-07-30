/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@SkipForDialect( dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner doesn't support stored procedures")
@DomainModel(
		annotatedClasses = {Person.class, Phone.class},
		xmlMappings = "org/hibernate/orm/test/procedure/PostgreSQLFunctionProcedureTest.xml"
)
@org.hibernate.testing.orm.junit.SessionFactory
public class PostgreSQLFunctionProcedureTest {

	@AfterEach
	public void cleanupTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	public void init(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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

	@Test
	public void testFunctionProcedureOutParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_count_phones", Long.class );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getSingleResult();
			assertEquals( 2, phoneCount );
		} );
	}

	@Test
	public void testFunctionProcedureRefCursor(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( 1, 1L );

			assertEquals( 2, query.getResultList().size() );
		} );
	}

	@Test
	public void testFunctionProcedureRefCursorOld(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( 2, 1L );

			assertEquals( 2, query.getResultList().size() );
		} );
	}

	@Test
	public void testFunctionWithJDBC(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long phoneCount = session.doReturningWork( connection -> {
				CallableStatement function = null;
				try {
					function = connection.prepareCall( "{ ? = call fn_count_phones(?) }" );
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
			assertEquals( 2, phoneCount );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			Integer value = null;

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
					value = resultSet.getInt( 1 );
				}
			}
			catch (Exception e) {
				fail( e.getMessage() );
			}
			assertEquals( 1, value );


			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			function.execute();

			value = (Integer) function.getSingleResult();

			assertEquals( 1, value );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameterOld(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			function.execute();

			assertFalse( function.hasMoreResults() );

			Integer value = null;
			try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
				while ( resultSet.next() ) {
					value = resultSet.getInt( 1 );
				}
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}

			assertEquals( 1, value );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testFunctionProcedureNullParameterHibernate(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( 1, null );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertTrue( result );
		} );

		factoryScope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( 1, "test" );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertFalse( result );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testFunctionProcedureNullParameterHibernateWithoutEnablePassingNulls(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( "param", null );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertTrue( result );
		} );
	}

	@Test
	public void testFunctionProcedureNullParameterHibernateWithoutSettingTheParameter(SessionFactoryScope factoryScope) {
		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
				() -> factoryScope.inTransaction( entityManager -> {
					ProcedureCall procedureCall = entityManager.unwrap( Session.class )
							.createStoredProcedureCall( "fn_is_null" );
					procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
					procedureCall.markAsFunctionCall( Boolean.class );

					procedureCall.execute();
				} ) );

		assertTrue( exception.getMessage().contains( "parameter named 'param'" ) );
	}
}
