package org.hibernate.event;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;

public class ReSaveReferencedDeletedEntityJPA extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Child.class, Parent.class };
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14416")
    public void testRefreshUnDeletedEntityWithReferencesJPA() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Parent parent = new Parent();
        parent.setId(1);

        Child child = new Child();
        child.setId(2);
        parent.setChild( child );

        em.unwrap(Session.class).save( parent );

        em.flush();

        em.remove( parent );

        em.detach( parent );

        em.persist( parent );

        em.refresh( child );
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14416")
    public void testReSaveDeletedEntityWithReferencesJPA() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Parent parent = new Parent();
        parent.setId(1);

        Child child = new Child();
        child.setId(2);
        parent.setChild( child );

        em.persist( parent );

        em.remove( child );

        em.unwrap(Session.class).save( child );
    }

    @Entity(name = "Child")
    public static class Child {
        @Id
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
