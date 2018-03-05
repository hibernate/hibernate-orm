package org.hibernate.test.stateless;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Vlad Mihalcea
 */
public class StatelessSessionConnectionTest extends BaseEntityManagerFunctionalTestCase {

    @Rule
    public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger( CoreMessageLogger.class, AbstractBatchImpl.class.getName() ) );

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Employee.class
        };
    }

    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.STATEMENT_BATCH_SIZE, 10 );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-11732" )
    public void test() {
        Triggerable triggerable = logInspection.watchForLogMessages( "HHH000352" );
        triggerable.reset();

        StatelessSession session = entityManagerFactory().unwrap( SessionFactory.class ).openStatelessSession();
        Transaction tx = session.beginTransaction();

        try {
            Employee employee = new Employee( "1", "2", 1 );
            employee.setId( 1 );
            session.insert( employee );

            tx.rollback();
        }
        catch (HibernateException e) {
            if ( tx != null ) {
                tx.rollback();
            }
        }
        finally {
            session.close();
            assertFalse( triggerable.wasTriggered() );
        }
    }

    @Entity(name = "Employee")
    public static class Employee {
        @Id
        private Integer id;

        private String firstName;

        private String lastName;

        private int salary;

        public Employee() {}

        public Employee(String fname, String lname, int salary) {
            this.firstName = fname;
            this.lastName = lname;
            this.salary = salary;
        }

        public Integer getId() {
            return id;
        }

        public void setId( Integer id ) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName( String first_name ) {
            this.firstName = first_name;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName( String last_name ) {
            this.lastName = last_name;
        }

        public int getSalary() {
            return salary;
        }

        public void setSalary( int salary ) {
            this.salary = salary;
        }
    }
}
