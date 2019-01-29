package org.hibernate.test.collection.nonInsertable;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.*;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@TestForIssue(jiraKey = "HHH-13236")
public class NonInsertableColumnTest extends BaseCoreFunctionalTestCase {


    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {
                Parent.class,
                Child.class
        };
    }

    @Override
    protected void configure(Configuration configuration) {
        super.configure( configuration );

        configuration.setProperty( AvailableSettings.SHOW_SQL, "true" );
    }

    @Test
    public void test() {
        Session session = null;
        Transaction transaction = null;

        try {
            session = openSession();
            transaction = session.beginTransaction();

            Child child = new Child();
            child.field = "Test";
            child.nonInsertable = "nonInsertable";
            child.nonUpdatable = "nonUpdatable";

            Parent parent = new Parent();
            parent.children = Arrays.asList(child);

            session.persist(parent);

            session.flush();

            transaction.commit();

            session.clear();

            Parent loaded = session.get(Parent.class, parent.id);
            assertEquals("nonUpdatable", loaded.children.get(0).nonUpdatable);
            assertNull(loaded.children.get(0).nonInsertable);
            assertEquals("Test", loaded.children.get(0).field);
            assertEquals("Test", loaded.children.get(0).shadowField);
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }

    @Entity(name="Parent")
    public static class Parent {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        public Long id;

        @ElementCollection
        public List<Child> children;
    }

    @Embeddable
    public static class Child {

        @Column(name="field")
        public String field;

        @Column(insertable = false)
        public String nonInsertable;

        @Column(updatable = false)
        public String nonUpdatable;

        @Column(name="field", insertable = false, updatable = false)
        public String shadowField;
    }

}
