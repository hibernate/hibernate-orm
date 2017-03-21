package org.hibernate.test.proxy;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;

import static org.hibernate.Hibernate.*;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.*;

public class HibernateUnproxyTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Parent.class, Child.class};
    }

    @Test
    public void testInitializedProxyCanBeUnproxied() {
        Parent p = new Parent();
        Child c = new Child();
        p.setChild(c);
        doInJPA(this::entityManagerFactory, (entityManager -> {
            entityManager.persist(p);
        }));
        doInJPA(this::entityManagerFactory, (entityManager -> {
            Parent parent = entityManager.find(Parent.class, p.getId());
            Child child = parent.getChild();
            assertFalse(isInitialized(child));
            initialize(child);
            Child unproxiedChild = (Child) unproxy(child);
            assertEquals(Child.class, unproxiedChild.getClass());
        }));
    }

    @Test
    public void testNotInitializedProxyCanBeUnproxiedWithInitialization() {
        Parent p = new Parent();
        Child c = new Child();
        p.setChild(c);
        doInJPA(this::entityManagerFactory, (entityManager -> {
            entityManager.persist(p);
        }));
        doInJPA(this::entityManagerFactory, (entityManager -> {
            Parent parent = entityManager.find(Parent.class, p.getId());
            Child child = parent.getChild();
            assertFalse(isInitialized(child));
            Child unproxiedChild = (Child) unproxy(child);
            assertTrue(isInitialized(child));
            assertEquals(Child.class, unproxiedChild.getClass());
        }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotHibernateProxyShouldThrowException() {
        Parent p = new Parent();
        Child c = new Child();
        p.setChild(c);
        doInJPA(this::entityManagerFactory, (entityManager -> {
            entityManager.persist(p);
        }));
        doInJPA(this::entityManagerFactory, (entityManager -> {
            Parent parent = entityManager.find(Parent.class, p.getId());
            unproxy(parent);
        }));
    }

    @Test
    public void testNullUnproxyReturnsNull() {
        assertNull(unproxy(null));
    }

    @Entity
    public static class Parent {
        @Id
        @GeneratedValue
        private Integer id;

        @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private Child child;

        public Integer getId() {
            return id;
        }

        public void setChild(Child child) {
            this.child = child;
            child.setParent(this);
        }

        public Child getChild() {
            return child;
        }
    }

    @Entity
    public static class Child {
        @Id
        @GeneratedValue
        private Integer id;

        @OneToOne(fetch = FetchType.LAZY)
        private Parent parent;

        public Integer getId() {
            return id;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }

        public Parent getParent() {
            return parent;
        }
    }
}
