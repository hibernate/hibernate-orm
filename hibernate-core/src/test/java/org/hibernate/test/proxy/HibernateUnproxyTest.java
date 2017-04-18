/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxy;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.*;

import java.util.Objects;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.*;

public class HibernateUnproxyTest extends BaseEntityManagerFunctionalTestCase {

    private static final Logger log = Logger.getLogger( HibernateUnproxyTest.class );

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
            assertFalse(Hibernate.isInitialized(child));
            Hibernate.initialize(child);
            Child unproxiedChild = (Child) Hibernate.unproxy(child);
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
            assertFalse(Hibernate.isInitialized(child));
            Child unproxiedChild = (Child) Hibernate.unproxy(child);
            assertTrue(Hibernate.isInitialized(child));
            assertEquals(Child.class, unproxiedChild.getClass());
        }));
    }

    @Test
    public void testNotHibernateProxyShouldThrowException() {
        Parent p = new Parent();
        Child c = new Child();
        p.setChild(c);
        doInJPA(this::entityManagerFactory, (entityManager -> {
            entityManager.persist(p);
        }));
        doInJPA(this::entityManagerFactory, (entityManager -> {
            Parent parent = entityManager.find(Parent.class, p.getId());
            assertSame(parent, Hibernate.unproxy(parent));
        }));
    }

    @Test
    public void testNullUnproxyReturnsNull() {
        assertNull(Hibernate.unproxy(null));
    }

    @Test
    public void testProxyEquality() {
        Parent parent = doInJPA(this::entityManagerFactory, (entityManager -> {
            Parent p = new Parent();
            p.name = "John Doe";
            entityManager.persist(p);
            return p;
        }));
        doInJPA(this::entityManagerFactory, (entityManager -> {
            Parent p = entityManager.getReference(Parent.class, parent.getId());
            assertFalse(parent.equals(p));
            assertTrue(parent.equals(Hibernate.unproxy(p)));
        }));
    }

    @Entity(name = "Parent")
    public static class Parent {
        @Id
        @GeneratedValue
        private Integer id;

        private String name;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parent parent = (Parent) o;
            return Objects.equals(name, parent.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Entity(name = "Child")
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
