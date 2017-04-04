package org.hibernate.test.procedure;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL81Dialect.class)
public class PostgreSQLStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

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
					statement.executeUpdate( "DROP FUNCTION sp_count_phones(bigint)" );
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
					statement.executeUpdate( "DROP FUNCTION fn_phones(bigint)" );
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
						"CREATE OR REPLACE FUNCTION sp_count_phones( " +
						"   IN personId bigint, " +
						"   OUT phoneCount bigint) " +
						"   RETURNS bigint AS " +
						"$BODY$ " +
						"    BEGIN " +
						"        SELECT COUNT(*) INTO phoneCount " +
						"        FROM phone  " +
						"        WHERE person_id = personId; " +
						"    END; " +
						"$BODY$ " +
						"LANGUAGE plpgsql;"
					);

					statement.executeUpdate(
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
						"LANGUAGE plpgsql"
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
	public void testStoredProcedureRefCursor() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "fn_phones" );
			query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR );
			query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN );

			query.setParameter( 2, 1L );

			List<Object[]> phones = query.getResultList();
			assertEquals( 2, phones.size() );
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
			Session session = entityManager.unwrap( Session.class );
			Long phoneCount = session.doReturningWork( new ReturningWork<Long>() {
				@Override
				public Long execute(Connection connection) throws SQLException {
					CallableStatement function = null;
					try {
						function = connection.prepareCall( "{ ? = call sp_count_phones(?) }" );
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
				}
			} );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	public void testFunctionWithJDBCByName() {
		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			Long phoneCount = session.doReturningWork( new ReturningWork<Long>() {
				@Override
				public Long execute(Connection connection) throws SQLException {
					CallableStatement function = null;
					try {
						function = connection.prepareCall( "{ ? = call sp_count_phones(?) }" );
						function.registerOutParameter( "phoneCount", Types.BIGINT );
						function.setLong( "personId", 1L );
						function.execute();
						return function.getLong( 1 );
					}
					finally {
						if ( function != null ) {
							function.close();
						}
					}
				}
			} );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} catch (Exception e) {
			assertEquals( SQLFeatureNotSupportedException.class, e.getCause().getClass() );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}
}
