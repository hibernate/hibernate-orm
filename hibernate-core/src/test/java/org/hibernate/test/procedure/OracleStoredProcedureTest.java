package org.hibernate.test.procedure;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.jdbc.Work;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(Oracle8iDialect.class)
public class OracleStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

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

        try {
            Session session = entityManager.unwrap( Session.class );

            session.doWork( new Work() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    Statement statement = null;
                    try {
                        statement = connection.createStatement();
                        statement.executeUpdate(
                                "CREATE OR REPLACE PROCEDURE sp_count_phones (  " +
                                        "   personId IN NUMBER,  " +
                                        "   phoneCount OUT NUMBER )  " +
                                        "AS  " +
                                        "BEGIN  " +
                                        "    SELECT COUNT(*) INTO phoneCount  " +
                                        "    FROM phone  " +
                                        "    WHERE person_id = personId; " +
                                        "END;"
                        );
                        statement.executeUpdate(
                                "CREATE OR REPLACE PROCEDURE sp_person_phones ( " +
                                        "   personId IN NUMBER, " +
                                        "   personPhones OUT SYS_REFCURSOR ) " +
                                        "AS  " +
                                        "BEGIN " +
                                        "    OPEN personPhones FOR " +
                                        "    SELECT *" +
                                        "    FROM phone " +
                                        "    WHERE person_id = personId; " +
                                        "END;"
                        );
                        statement.executeUpdate(
                                "CREATE OR REPLACE FUNCTION fn_count_phones ( " +
                                        "    personId IN NUMBER ) " +
                                        "    RETURN NUMBER " +
                                        "IS " +
                                        "    phoneCount NUMBER; " +
                                        "BEGIN " +
                                        "    SELECT COUNT(*) INTO phoneCount " +
                                        "    FROM phone " +
                                        "    WHERE person_id = personId; " +
                                        "    RETURN( phoneCount ); " +
                                        "END;"
                        );
                        statement.executeUpdate(
                                "CREATE OR REPLACE FUNCTION fn_person_and_phones ( " +
                                        "    personId IN NUMBER ) " +
                                        "    RETURN SYS_REFCURSOR " +
                                        "IS " +
                                        "    personAndPhones SYS_REFCURSOR; " +
                                        "BEGIN " +
                                        "   OPEN personAndPhones FOR " +
                                        "        SELECT " +
                                        "            pr.id AS \"pr.id\", " +
                                        "            pr.name AS \"pr.name\", " +
                                        "            pr.nickName AS \"pr.nickName\", " +
                                        "            pr.address AS \"pr.address\", " +
                                        "            pr.createdOn AS \"pr.createdOn\", " +
                                        "            pr.version AS \"pr.version\", " +
                                        "            ph.id AS \"ph.id\", " +
                                        "            ph.person_id AS \"ph.person_id\", " +
                                        "            ph.phone_number AS \"ph.phone_number\" " +
                                        "       FROM person pr " +
                                        "       JOIN phone ph ON pr.id = ph.person_id " +
                                        "       WHERE pr.id = personId; " +
                                        "   RETURN personAndPhones; " +
                                        "END;"
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
            Person person1 = new Person("John Doe" );
            person1.setNickName( "JD" );
            person1.setAddress( "Earth" );
            person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) )) ;

            entityManager.persist(person1);

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
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP PROCEDURE sp_count_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        }
        finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
        }

        entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        try {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP PROCEDURE sp_person_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        }
        finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
        }

        entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        try {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP FUNCTION fn_count_phones");
                }
                catch (SQLException ignore) {
                }
            } );
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
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_count_phones");
            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);

            query.setParameter(1, 1L);

            query.execute();
            Long phoneCount = (Long) query.getOutputParameterValue(2);
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
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_person_phones" );
            query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
            query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
            query.setParameter( 1, 1L );

            query.execute();
            List<Object[]> postComments = query.getResultList();
            assertNotNull( postComments );
        }
        finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
        }
    }

    @Test
    public void testHibernateProcedureCallRefCursor() {
        EntityManager entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        try {
            Session session = entityManager.unwrap(Session.class);

            ProcedureCall call = session.createStoredProcedureCall( "sp_person_phones");
            call.registerParameter(1, Long.class, ParameterMode.IN).bindValue(1L);
            call.registerParameter(2, Class.class, ParameterMode.REF_CURSOR);

            Output output = call.getOutputs().getCurrent();
            List<Object[]> postComments = ( (ResultSetOutput) output ).getResultList();
            assertEquals(2, postComments.size());
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
            BigDecimal phoneCount = (BigDecimal) entityManager
                .createNativeQuery("SELECT fn_count_phones(:personId) FROM DUAL")
                .setParameter("personId", 1)
                .getSingleResult();
            assertEquals(BigDecimal.valueOf(2), phoneCount);
        }
        finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
        }
    }

    @Test
    @FailureExpected( jiraKey = "HHH-10898")
    public void testNamedNativeQueryStoredProcedureRefCursor() {
        EntityManager entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        try {
            List<Object[]> postAndComments = entityManager
                    .createNamedQuery(
                            "fn_person_and_phones")
                    .setParameter(1, 1L)
                    .getResultList();
            Object[] postAndComment = postAndComments.get(0);
            Person person = (Person) postAndComment[0];
            Phone phone = (Phone) postAndComment[1];
            assertEquals(2, postAndComments.size());
        }
        finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
        }
    }

    @Test
    public void testNamedNativeQueryStoredProcedureRefCursorWithJDBC() {
        EntityManager entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        try {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try (CallableStatement function = connection.prepareCall(
                        "{ ? = call fn_person_and_phones( ? ) }" )) {
                    try {
                        function.registerOutParameter( 1, Types.REF_CURSOR );
                    }
                    catch ( SQLException e ) {
                        //OracleTypes.CURSOR
                        function.registerOutParameter( 1, -10 );
                    }
                    function.setInt( 2, 1 );
                    function.execute();
                    try (ResultSet resultSet = (ResultSet) function.getObject( 1);) {
                        while (resultSet.next()) {
                            Long postCommentId = resultSet.getLong(1);
                            String review = resultSet.getString(2);
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
