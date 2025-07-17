/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSQLFunctionProcedureTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@Override
	protected void applyMetadataImplementor(MetadataImplementor metadataImplementor) {
		final Database database = metadataImplementor.getDatabase();
		final Namespace namespace = database.getDefaultNamespace();
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"fn_count_phones",
						namespace,
						"CREATE OR REPLACE FUNCTION fn_count_phones( " +
								"   IN personId bigint) " +
								"   RETURNS bigint AS " +
								"$BODY$ " +
								"    DECLARE " +
								"        phoneCount bigint; " +
								"    BEGIN " +
								"        SELECT COUNT(*) INTO phoneCount " +
								"        FROM phone  " +
								"        WHERE person_id = personId; " +
								"        RETURN phoneCount;" +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;",
						"drop function fn_count_phones(bigint)",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"fn_phones",
						namespace,
						"CREATE OR REPLACE FUNCTION fn_phones(personId BIGINT) " +
								"   RETURNS REFCURSOR AS " +
								"$BODY$ " +
								"    DECLARE " +
								"        phones REFCURSOR; " +
								"    BEGIN " +
								"        OPEN phones FOR  " +
								"            SELECT *  " +
								"            FROM phone   " +
								"            WHERE person_id = personId;  " +
								"        RETURN phones; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql",
						"drop function fn_phones(bigint, bigint)",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"singleRefCursor",
						namespace,
						"CREATE OR REPLACE FUNCTION singleRefCursor() " +
								"   RETURNS REFCURSOR AS " +
								"$BODY$ " +
								"    DECLARE " +
								"        p_recordset REFCURSOR; " +
								"    BEGIN " +
								"      OPEN p_recordset FOR SELECT 1; " +
								"      RETURN p_recordset; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;",
						"drop function singleRefCursor()",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"fn_is_null",
						namespace,
						"CREATE OR REPLACE FUNCTION fn_is_null( " +
								"   IN param varchar(255)) " +
								"   RETURNS boolean AS " +
								"$BODY$ " +
								"    DECLARE " +
								"        result boolean; " +
								"    BEGIN " +
								"        SELECT param is null INTO result; " +
								"        RETURN result; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;",
						"drop function fn_is_null(varchar)",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testFunctionProcedureOutParameter() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_count_phones", Long.class );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getSingleResult();
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	public void testFunctionProcedureRefCursor() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( 1, 1L );

			List<Object[]> phones = query.getResultList();
			assertEquals( 2, phones.size() );
		} );
	}

	@Test
	public void testFunctionProcedureRefCursorOld() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( 2, 1L );

			List<Object[]> phones = query.getResultList();
			assertEquals( 2, phones.size() );
		} );
	}

	@Test
	public void testFunctionWithJDBC() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameter() {

		doInJPA( this::entityManagerFactory, entityManager -> {
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
			assertEquals( Integer.valueOf( 1 ), value );


			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			function.execute();

			value = (Integer) function.getSingleResult();

			assertEquals( Integer.valueOf( 1 ), value );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameterOld() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			function.execute();

			assertFalse( function.hasMoreResults() );

			Integer value = null;
			try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 ) ) {
				while ( resultSet.next() ) {
					value = resultSet.getInt( 1 );
				}
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}

			assertEquals( Integer.valueOf( 1 ), value );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testFunctionProcedureNullParameterHibernate() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( 1, null );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertTrue( result );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testFunctionProcedureNullParameterHibernateWithoutEnablePassingNulls() {

		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testFunctionProcedureNullParameterHibernateWithoutSettingTheParameter() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				ProcedureCall procedureCall = entityManager.unwrap( Session.class )
						.createStoredProcedureCall( "fn_is_null" );
				procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
				procedureCall.markAsFunctionCall( Boolean.class );

				procedureCall.execute();

				fail( "Should have thrown exception" );
			}
			catch (IllegalArgumentException e) {
				assertEquals(
						"The parameter named [param] was not set! You need to call the setParameter method.",
						e.getMessage()
				);
			}
		} );
	}
}
