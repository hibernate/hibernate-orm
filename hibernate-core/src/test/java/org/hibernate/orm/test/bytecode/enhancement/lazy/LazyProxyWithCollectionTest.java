/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Christian Beikov
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-14619" )
@RunWith( BytecodeEnhancerRunner.class )
public class LazyProxyWithCollectionTest extends BaseCoreFunctionalTestCase {

    private Long childId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Parent.class, Child.class};
    }

    @Before
    public void prepare() {
        doInJPA( this::sessionFactory, em -> {
            Child c = new Child();
            em.persist( c );
            childId = c.getId();
        } );
    }

    @Test
    public void testReference() {
        doInJPA( this::sessionFactory, em -> {
            Child child = em.getReference( Child.class, childId );
            Parent parent = new Parent();
            parent.child = child;
            em.persist( parent );
            // Class cast exception occurs during auto-flush
            em.find( Parent.class, parent.getId() );
        } );
    }

    @Test
    public void testLazyCollection() {
        doInJPA( this::sessionFactory, em -> {
            Child child = em.find( Child.class, childId );
            Parent parent = new Parent();
            parent.child = child;
            em.persist( parent );
            child.children = new HashSet<>();
            // Class cast exception occurs during auto-flush
            em.find( Parent.class, parent.getId() );
        } );
    }

    @Test
    @Jira( "https://hibernate.atlassian.net/browse/HHH-17750" )
    public void testMerge() {
        final Child child = doInJPA( this::sessionFactory, em -> {
            return em.find( Child.class, childId );
        } );

        final Parent parent = doInJPA( this::sessionFactory, em -> {
            Parent p = new Parent();
            p.setChild( child );
            return em.merge( p );
        } );

        doInJPA( this::sessionFactory, em -> {
            em.merge( parent );
        } );

        doInJPA( this::sessionFactory, em -> {
            assertThat( em.find( Parent.class, parent.getId() ).getChild().getId() ).isEqualTo( child.getId() );
        } );
    }

    // --- //

    @Entity
    @Table( name = "PARENT" )
    private static class Parent {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @OneToOne( fetch = FetchType.LAZY )
        Child child;

        public Long getId() {
            return id;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    @Entity
    @Table( name = "CHILD" )
    private static class Child {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;
        @Version
        Long version;

        String name;

        @OneToMany
        Set<Child> children = new HashSet<>();

        Child() {
            // No-arg constructor necessary for proxy factory
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<Child> getChildren() {
            return children;
        }

        public void setChildren(Set<Child> children) {
            this.children = children;
        }
    }

}
