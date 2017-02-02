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
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(SQLServer2012Dialect.class)
public class SQLServerStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
		};
	}

	@Before
	public void init() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();
		Session session = entityManager.unwrap( Session.class );

		session.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP PROCEDURE sp_count_phones" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			}
		} );

		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = createEntityManager();
		entityManager.getTransaction().begin();
		session = entityManager.unwrap( Session.class );

		session.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP FUNCTION fn_count_phones" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			}
		} );

		entityManager.getTransaction().commit();
		entityManager.close();
		
		entityManager = createEntityManager();
		entityManager.getTransaction().begin();
		session = entityManager.unwrap( Session.class );

		session.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate( "DROP PROCEDURE sp_phones" );
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			}
		} );

		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = createEntityManager();
		entityManager.getTransaction().begin();
		session = entityManager.unwrap( Session.class );

		session.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate(
						"CREATE PROCEDURE sp_count_phones " +
						"   @personId INT, " +
						"   @phoneCount INT OUTPUT " +
						"AS " +
						"BEGIN " +
						"   SELECT @phoneCount = COUNT(*)  " +
						"   FROM Phone  " +
						"   WHERE person_id = @personId " +
						"END"
					);

					statement.executeUpdate(
						"CREATE FUNCTION fn_count_phones (@personId INT)  " +
						"RETURNS INT  " +
						"AS  " +
						"BEGIN  " +
						"    DECLARE @phoneCount int;  " +
						"    SELECT @phoneCount = COUNT(*) " +
						"    FROM Phone   " +
						"    WHERE person_id = @personId;  " +
						"    RETURN(@phoneCount);  " +
						"END"
					);

					statement.executeUpdate(
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
						"    OPEN @phones;"
					);
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			}
		} );

		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = createEntityManager();
		entityManager.getTransaction().begin();

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
		entityManager.close();
	}

	@Test
	public void testStoredProcedureOutParameter() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_count_phones");
			query.registerStoredProcedureParameter("personId", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("phoneCount", Long.class, ParameterMode.OUT);

			query.setParameter("personId", 1L);

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue("phoneCount");
			assertEquals(Long.valueOf(2), phoneCount);
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
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_phones");
			query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter(2, Class.class, ParameterMode.REF_CURSOR);
			query.setParameter(1, 1L);

			query.execute();
			List<Object[]> postComments = query.getResultList();
			assertNotNull(postComments);
		}
		catch (Exception e) {
			assertTrue( Pattern.compile( "Dialect .*? not known to support REF_CURSOR parameters").matcher( e.getCause().getMessage()).matches());
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
			Session session = entityManager.unwrap( Session.class );
			session.doWork( new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					CallableStatement function = null;
					try {
						function = connection.prepareCall("{ ? = call fn_count_phones(?) }");
						function.registerOutParameter(1, Types.INTEGER);
						function.setInt(2, 1);
						function.execute();
						int phoneCount = function.getInt(1);
						assertEquals(2, phoneCount);
					}
					finally {
						if ( function != null ) {
							function.close();
						}
					}
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}
}
