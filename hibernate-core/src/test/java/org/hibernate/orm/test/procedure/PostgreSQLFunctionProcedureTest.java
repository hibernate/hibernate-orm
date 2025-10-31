/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSQLFunctionProcedureTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@AfterEach
	public void cleanupTestData() {
		entityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		EntityManagerFactoryBuilder entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				buildSettings()
		);
		Database database = entityManagerFactoryBuilder.metadata().getDatabase();
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
		return entityManagerFactoryBuilder.build();
	}

	@BeforeEach
	public void init() {
		inTransaction( entityManager -> {
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
		inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_count_phones", Long.class );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getSingleResult();
			assertThat( phoneCount ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testFunctionProcedureRefCursor() {
		inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( 1, 1L );

			assertThat( query.getResultList() ).hasSize( 2 );
		} );
	}

	@Test
	public void testFunctionProcedureRefCursorOld() {
		inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			query.setParameter( 2, 1L );

			assertThat( query.getResultList() ).hasSize( 2 );
		} );
	}

	@Test
	public void testFunctionWithJDBC() {
		inTransaction( entityManager -> {
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
			assertThat( phoneCount ).isEqualTo( 2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameter() {

		inTransaction( entityManager -> {
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
			assertThat( value ).isEqualTo( 1 );


			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			function.execute();

			value = (Integer) function.getSingleResult();

			assertThat( value ).isEqualTo( 1 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11863")
	public void testSysRefCursorAsOutParameterOld() {
		inTransaction( entityManager -> {
			StoredProcedureQuery function = entityManager.createStoredProcedureQuery( "singleRefCursor" );
			function.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			function.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );

			function.execute();

			assertThat( function.hasMoreResults() ).isFalse();

			Integer value = null;
			try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
				while ( resultSet.next() ) {
					value = resultSet.getInt( 1 );
				}
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}

			assertThat( value ).isEqualTo( 1 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testFunctionProcedureNullParameterHibernate() {

		inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( 1, null );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertThat( result ).isTrue();
		} );

		inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( 1, "test" );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertThat( result ).isFalse();
		} );
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testFunctionProcedureNullParameterHibernateWithoutEnablePassingNulls() {

		inTransaction( entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "fn_is_null" );
			procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.markAsFunctionCall( Boolean.class );
			procedureCall.setParameter( "param", null );

			Boolean result = (Boolean) procedureCall.getSingleResult();

			assertThat( result ).isTrue();
		} );
	}

	@Test
	public void testFunctionProcedureNullParameterHibernateWithoutSettingTheParameter() {

		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
				() -> inTransaction( entityManager -> {
					ProcedureCall procedureCall = entityManager.unwrap( Session.class )
							.createStoredProcedureCall( "fn_is_null" );
					procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
					procedureCall.markAsFunctionCall( Boolean.class );

					procedureCall.execute();
				} ) );

		assertThat( exception.getMessage() )
				.isEqualTo( "The parameter named [param] was not set! You need to call the setParameter method." );
	}
}
