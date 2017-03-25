package org.hibernate.userguide.sql;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Session;
import org.hibernate.dialect.Oracle8iDialect;
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
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(Oracle8iDialect.class)
public class OracleStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

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
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
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
                    //tag::sql-sp-ref-cursor-oracle-example[]
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
                    //end::sql-sp-ref-cursor-oracle-example[]
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
                    statement.executeUpdate("DROP PROCEDURE sp_count_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        });
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP PROCEDURE sp_person_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        });
        doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP FUNCTION fn_count_phones");
                }
                catch (SQLException ignore) {
                }
            } );
        });
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_count_phones");
            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);

            query.setParameter(1, 1L);

            query.execute();
            Long phoneCount = (Long) query.getOutputParameterValue(2);
            assertEquals(Long.valueOf(2), phoneCount);
        });
    }

    @Test
    public void testStoredProcedureRefCursor() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-jpa-call-sp-ref-cursor-oracle-example[]
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_person_phones" );
            query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
            query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
            query.setParameter( 1, 1L );

            query.execute();
            List<Object[]> postComments = query.getResultList();
            //end::sql-jpa-call-sp-ref-cursor-oracle-example[]
            assertNotNull( postComments );
        });
    }

    @Test
    public void testHibernateProcedureCallRefCursor() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::sql-hibernate-call-sp-ref-cursor-oracle-example[]
            Session session = entityManager.unwrap(Session.class);
            
            ProcedureCall call = session.createStoredProcedureCall( "sp_person_phones");
            call.registerParameter(1, Long.class, ParameterMode.IN).bindValue(1L);
            call.registerParameter(2, Class.class, ParameterMode.REF_CURSOR);

            Output output = call.getOutputs().getCurrent();
            List<Object[]> postComments = ( (ResultSetOutput) output ).getResultList();
            assertEquals(2, postComments.size());
            //end::sql-hibernate-call-sp-ref-cursor-oracle-example[]
        });
    }

    @Test
    public void testStoredProcedureReturnValue() {
        try {
            doInJPA( this::entityManagerFactory, entityManager -> {
				BigDecimal phoneCount = (BigDecimal) entityManager
						.createNativeQuery("SELECT fn_count_phones(:personId) FROM DUAL")
						.setParameter("personId", 1)
						.getSingleResult();
				assertEquals(BigDecimal.valueOf(2), phoneCount);
			});
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
