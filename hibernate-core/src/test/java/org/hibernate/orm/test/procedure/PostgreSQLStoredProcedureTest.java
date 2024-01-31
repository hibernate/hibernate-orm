/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.hibernate.Session;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQLDialect.class)
public class PostgreSQLStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

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
						"sp_count_phones",
						namespace,
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
								"LANGUAGE plpgsql;",
						"DROP PROCEDURE sp_count_phones(bigint, bigint)",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"sp_phones",
						namespace,
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
								"LANGUAGE plpgsql",
						"DROP PROCEDURE sp_phones(bigint, refcursor)",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"singleRefCursor",
						namespace,
						"CREATE OR REPLACE PROCEDURE singleRefCursor(INOUT p_recordset REFCURSOR) " +
								"   AS " +
								"$BODY$ " +
								"    BEGIN " +
								"      OPEN p_recordset FOR SELECT 1; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;",
						"DROP PROCEDURE singleRefCursor(refcursor)",
						Set.of( PostgreSQLDialect.class.getName(), PostgresPlusDialect.class.getName() )
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"sp_is_null",
						namespace,
						"CREATE OR REPLACE PROCEDURE sp_is_null( " +
								"   IN param varchar(255), " +
								"   INOUT result boolean) " +
								"   AS " +
								"$BODY$ " +
								"    BEGIN " +
								"    select param is null into result; " +
								"    END; " +
								"$BODY$ " +
								"LANGUAGE plpgsql;",
						"DROP PROCEDURE sp_is_null(varchar, boolean)",
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
	public void testStoredProcedureOutParameter() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testStoredProcedureRefCursor() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( 2, void.class, ParameterMode.REF_CURSOR );

			query.setParameter( 1, 1L );

			List<Object[]> phones = query.getResultList();
			assertEquals( 2, phones.size() );
		} );
	}

	@Test
	public void testStoredProcedureWithJDBC() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testProcedureWithJDBCByName() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				Session session = entityManager.unwrap( Session.class );
				Long phoneCount = session.doReturningWork( connection -> {
					CallableStatement procedure = null;
					try {
						procedure = connection.prepareCall( "{ call sp_count_phones(?,?) }" );
						procedure.registerOutParameter( "phoneCount", Types.BIGINT );
						procedure.setLong( "personId", 1L );
						procedure.execute();
						return procedure.getLong( 1 );
					}
					finally {
						if ( procedure != null ) {
							procedure.close();
						}
					}
				} );
				assertEquals( Long.valueOf( 2 ), phoneCount );
			}
			catch (Exception e) {
				assertEquals( SQLFeatureNotSupportedException.class, e.getCause().getClass() );
			}
		} );
	}

	@Test
	@JiraKey("HHH-11863")
	@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14, comment = "Stored procedure OUT parameters are only supported since version 14")
	public void testSysRefCursorAsOutParameter() {

		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testStoredProcedureNullParameterHibernate() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			ProcedureCall procedureCall = entityManager.unwrap( Session.class )
					.createStoredProcedureCall( "sp_is_null" );
			procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
			procedureCall.registerParameter( 2, Boolean.class, ParameterMode.INOUT );
			procedureCall.setParameter( 1, null );
			procedureCall.setParameter( 2, null );

			Boolean result = (Boolean) procedureCall.getOutputParameterValue( 2 );

			assertTrue( result );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testStoredProcedureNullParameterHibernateWithoutEnablePassingNulls() {

		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testStoredProcedureNullParameterHibernateWithoutSettingTheParameter() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				ProcedureCall procedureCall = entityManager.unwrap( Session.class )
						.createStoredProcedureCall( "sp_is_null" );
				procedureCall.registerParameter( "param", StandardBasicTypes.STRING, ParameterMode.IN );
				procedureCall.registerParameter( "result", Boolean.class, ParameterMode.OUT );

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
