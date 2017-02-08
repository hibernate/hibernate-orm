package org.hibernate.userguide.sql;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.userguide.model.AddressType;
import org.hibernate.userguide.model.Call;
import org.hibernate.userguide.model.Partner;
import org.hibernate.userguide.model.Person;
import org.hibernate.userguide.model.Phone;
import org.hibernate.userguide.model.PhoneType;

import org.hibernate.testing.RequiresDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
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
                Partner.class,
                Phone.class,
                Call.class,
        };
    }

    @Before
    public void init() {
        destroy();
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    //tag::sql-sp-out-mysql-example[]
                    statement.executeUpdate(
                        "CREATE PROCEDURE sp_count_phones (" +
                        "   IN personId INT, " +
                        "   OUT phoneCount INT " +
                        ") " +
                        "BEGIN " +
                        "    SELECT COUNT(*) INTO phoneCount " +
                        "    FROM Phone p " +
                        "    WHERE p.person_id = personId; " +
                        "END"
                    );
                    //end::sql-sp-out-mysql-example[]
                    //tag::sql-sp-no-out-mysql-example[]
                    statement.executeUpdate(
                        "CREATE PROCEDURE sp_phones(IN personId INT) " +
                        "BEGIN " +
                        "    SELECT *  " +
                        "    FROM Phone   " +
                        "    WHERE person_id = personId;  " +
                        "END"
                    );
                    //end::sql-sp-no-out-mysql-example[]
                    //tag::sql-function-mysql-example[]
                    statement.executeUpdate(
                        "CREATE FUNCTION fn_count_phones(personId integer)  " +
                        "RETURNS integer " +
                        "DETERMINISTIC " +
                        "READS SQL DATA " +
                        "BEGIN " +
                        "    DECLARE phoneCount integer; " +
                        "    SELECT COUNT(*) INTO phoneCount " +
                        "    FROM Phone p " +
                        "    WHERE p.person_id = personId; " +
                        "    RETURN phoneCount; " +
                        "END"
                    );
                    //end::sql-function-mysql-example[]
                }
            } );
        });
        doInJPA( this::entityManagerFactory, entityManager -> {
            Person person1 = new Person("John Doe" );
            person1.setNickName( "JD" );
            person1.setAddress( "Earth" );
            person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) )) ;
            person1.getAddresses().put( AddressType.HOME, "Home address" );
            person1.getAddresses().put( AddressType.OFFICE, "Office address" );

            entityManager.persist(person1);

            Phone phone1 = new Phone( "123-456-7890" );
            phone1.setId( 1L );
            phone1.setType( PhoneType.MOBILE );

            person1.addPhone( phone1 );

            Phone phone2 = new Phone( "098_765-4321" );
            phone2.setId( 2L );
            phone2.setType( PhoneType.LAND_LINE );

            person1.addPhone( phone2 );
        });
    }

    @After
    public void destroy() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP PROCEDURE IF EXISTS sp_count_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        });
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP PROCEDURE IF EXISTS sp_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        });
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP FUNCTION IF EXISTS fn_count_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        });
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-jpa-call-sp-out-mysql-example[]
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones");
            query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT);

            query.setParameter("personId", 1L);

            query.execute();
            Long phoneCount = (Long) query.getOutputParameterValue("phoneCount");
            //end::sql-jpa-call-sp-out-mysql-example[]
            assertEquals(Long.valueOf(2), phoneCount);
        });
    }

    @Test
    public void testHibernateProcedureCallOutParameter() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-hibernate-call-sp-out-mysql-example[]
            Session session = entityManager.unwrap( Session.class );

            ProcedureCall call = session.createStoredProcedureCall( "sp_count_phones" );
            call.registerParameter( "personId", Long.class, ParameterMode.IN ).bindValue( 1L );
            call.registerParameter( "phoneCount", Long.class, ParameterMode.OUT );

            Long phoneCount = (Long) call.getOutputs().getOutputParameterValue( "phoneCount" );
            assertEquals( Long.valueOf( 2 ), phoneCount );
            //end::sql-hibernate-call-sp-out-mysql-example[]
        });
    }

    @Test
    public void testStoredProcedureRefCursor() {
        try {
            doInJPA( this::entityManagerFactory, entityManager -> {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones");
                query.registerStoredProcedureParameter( 1, void.class, ParameterMode.REF_CURSOR);
                query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.IN);

                query.setParameter(2, 1L);

                List<Object[]> personComments = query.getResultList();
                assertEquals(2, personComments.size());
            });
        } catch (Exception e) {
            assertTrue(Pattern.compile("Dialect .*? not known to support REF_CURSOR parameters").matcher(e.getCause().getMessage()).matches());
        }
    }

    @Test
    public void testStoredProcedureReturnValue() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-jpa-call-sp-no-out-mysql-example[]
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones");
            query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN);

            query.setParameter(1, 1L);

            List<Object[]> personComments = query.getResultList();
            //end::sql-jpa-call-sp-no-out-mysql-example[]
            assertEquals(2, personComments.size());
        });
    }

    @Test
    public void testHibernateProcedureCallReturnValueParameter() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-hibernate-call-sp-no-out-mysql-example[]
            Session session = entityManager.unwrap( Session.class );

            ProcedureCall call = session.createStoredProcedureCall( "sp_phones" );
            call.registerParameter( 1, Long.class, ParameterMode.IN ).bindValue( 1L );

            Output output = call.getOutputs().getCurrent();

            List<Object[]> personComments = ( (ResultSetOutput) output ).getResultList();
            //end::sql-hibernate-call-sp-no-out-mysql-example[]
            assertEquals( 2, personComments.size() );
        });
    }

    @Test
    public void testFunctionWithJDBC() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-call-function-mysql-example[]
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
            //end::sql-call-function-mysql-example[]
            assertEquals(Integer.valueOf(2), phoneCount.get());
        });
    }
}
