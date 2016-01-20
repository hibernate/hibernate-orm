package org.hibernate.test.hql;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author Johannes Buehler
 */

public class CoalesceStatementTest extends BaseCoreFunctionalTestCase {

    private Person person;

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {
                Person.class
        };
    }


    // Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
    @Override
    protected void configure(Configuration configuration) {
        super.configure( configuration );
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL9Dialect");
        configuration.setProperty("hibernate.connection.driver_class","org.postgresql.Driver");
        configuration.setProperty("hibernate.connection.url","jdbc:postgresql://localhost/pg_coalesce");
        configuration.setProperty("hibernate.connection.username","sa");

    }

    @Before
    public void setUp() {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        person = new Person();
        person.setName("Johannes");
        person.setSurname("Buehler");
        session.persist(person);
        tx.commit();
        s.close();
    }

    // Add your tests, using standard JUnit.
    @Test
    public void hhh123TestCoalesce() throws Exception {
        // BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
        Session s = openSession();

        try {
            Query query = session.createQuery("from Person p where p.name = coalesce(:name , p.name) ");
            query.setParameter("name", "Johannes");
            List<Person> resultList = query.list();
            assertThat(resultList, hasItem(person));
        } finally {
            session.close();
        }

    }
    @Test
    public void hhh123TestNullInCoalesce() throws Exception {
        // BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
        Session s = openSession();

        try {
            Query query = session.createQuery("from Person p where p.name = coalesce(:name , p.name) ");
            query.setParameter("name", null);
            List<Person> resultList = query.list();
            assertThat(resultList, hasItem(person));
        } finally {
            session.close();
        }

    }

}