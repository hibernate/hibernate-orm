package org.hibernate.event;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

public class ReSaveReferencedDeletedEntity extends BaseCoreFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Child.class, Parent.class };
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14416")
    public void testReSaveDeletedEntityWithReferences() {
        doInHibernate( this::sessionFactory, session -> {
            Parent parent = new Parent();

            Child child = new Child();
            parent.setChild( child );

            session.saveOrUpdate( parent );
            session.saveOrUpdate( child );

            session.remove(child);

            session.save(child);
        } );
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14416")
    public void testReSaveDeletedEntityWithReferences2() {
        doInHibernate( this::sessionFactory, session -> {
            Parent parent = new Parent();

            Child child = new Child();
            parent.setChild( child );

            session.saveOrUpdate( parent );
            session.saveOrUpdate( child );

            session.remove(child);

            session.detach(child);

            session.save(child);
        } );
    }

    @Entity(name = "Child")
    public static class Child {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    @Entity(name = "Parent")
    public static class Parent {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @OneToOne(cascade = CascadeType.ALL)
        private Child child;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }
}
