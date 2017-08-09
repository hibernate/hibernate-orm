/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.cascade;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10252" )
@RunWith( BytecodeEnhancerRunner.class )
public class CascadeDeleteTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{Parent.class, Child.class};
    }

    @Before
    public void prepare() {
        // Create a Parent with one Child
        doInHibernate( this::sessionFactory, s -> {
                    Parent p = new Parent();
                    p.setName( "PARENT" );
                    p.setLazy( "LAZY" );
                    p.makeChild();
                    s.persist( p );
                }
        );
    }

    @Test
    public void test() {
        // Delete the Parent
        doInHibernate( this::sessionFactory, s -> {
            Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
                    .setParameter( "name", "PARENT" )
                    .uniqueResult();

            s.delete( loadedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    // --- //

    @Entity( name = "Parent" )
    @Table( name = "PARENT" )
    public static class Parent {

        Long id;

        String name;

        List<Child> children = new ArrayList<>();

        String lazy;

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        @OneToMany( mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY )
        List<Child> getChildren() {
            return Collections.unmodifiableList( children );
        }

        void setChildren(List<Child> children) {
            this.children = children;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        @Basic( fetch = FetchType.LAZY )
        String getLazy() {
            return lazy;
        }

        void setLazy(String lazy) {
            this.lazy = lazy;
        }

        void makeChild() {
            Child c = new Child();
            c.setParent( this );
            children.add( c );
        }
    }

    @Entity
    @Table( name = "CHILD" )
    private static class Child {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @ManyToOne( optional = false )
        @JoinColumn( name = "parent_id" )
        Parent parent;

        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        Parent getParent() {
            return parent;
        }

        void setParent(Parent parent) {
            this.parent = parent;
        }
    }
}
