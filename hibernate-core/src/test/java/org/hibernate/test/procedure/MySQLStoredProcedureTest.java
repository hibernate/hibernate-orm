package org.hibernate.test.procedure;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.RequiresDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQL5Dialect.class)
public class MySQLStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Before
	public void init() {
		destroy();

		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
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
										"    FROM phone  " +
										"    WHERE phone.person_id = personId; " +
										"END"
						);
						statement.executeUpdate(
								"CREATE PROCEDURE sp_phones(IN personId INT) " +
										"BEGIN " +
										"    SELECT *  " +
										"    FROM phone   " +
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
										"    FROM phone  " +
										"    WHERE phone.person_id = personId; " +
										"    RETURN phoneCount; " +
										"END"
						);
					} finally {
						if ( statement != null ) {
							statement.close();
						}
					}
				}
			}  );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Person person1 = new Person( "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );

			person1.addPhone( phone2 );

			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	@After
	public void destroy() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_count_phones" );
					}
					catch (SQLException ignore) {
					}
				}
			}  );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate( "DROP PROCEDURE IF EXISTS sp_phones" );
					}
					catch (SQLException ignore) {
					}
				}
			}  );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate( "DROP FUNCTION IF EXISTS fn_count_phones" );
					}
					catch (SQLException ignore) {
					}
				}
			}  );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testStoredProcedureOutParameter() {

		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testHibernateProcedureCallOutParameter() {

		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );

			ProcedureCall call = session.createStoredProcedureCall( "sp_count_phones" );
			call.registerParameter( "personId", Long.class, ParameterMode.IN ).bindValue( 1L );
			call.registerParameter( "phoneCount", Long.class, ParameterMode.OUT );

			Long phoneCount = (Long) call.getOutputs().getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testStoredProcedureRefCursor() {

		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();
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
								.matcher( e.getCause().getMessage() )
								.matches() );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testStoredProcedureReturnValue() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );

			query.setParameter( 1, 1L );

			List<Object[]> personComments = query.getResultList();
			assertEquals( 2, personComments.size() );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testHibernateProcedureCallReturnValueParameter() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );

			ProcedureCall call = session.createStoredProcedureCall( "sp_phones" );
			call.registerParameter( 1, Long.class, ParameterMode.IN ).bindValue( 1L );

			Output output = call.getOutputs().getCurrent();

			List<Object[]> personComments = ( (ResultSetOutput) output ).getResultList();
			assertEquals( 2, personComments.size() );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testFunctionWithJDBC() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
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
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}
}
