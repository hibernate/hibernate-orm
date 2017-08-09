/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue( jiraKey = "HHH-10708" )
@RunWith( BytecodeEnhancerRunner.class )
public class UnexpectedDeleteTest3 extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{Parent.class, Child.class};
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Child child = new Child();
            child.setId( 2L );
            s.save( child );

            Parent parent = new Parent();
            parent.setId( 1L );
            parent.setNames( Collections.singleton( "name" ) );
            parent.addChild( child );

            s.save( parent );
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            Parent parent = s.get( Parent.class, 1L );
           
            Child child = new Child();
            child.setId( 1L );
            s.save( child );
            parent.addChild( child );

            // We need to leave at least one attribute unfetchd
            //parent.getNames().size();
            s.save( parent );
        } );

        doInHibernate( this::sessionFactory, s -> {
            Parent application = s.get( Parent.class, 1L );
            Assert.assertEquals( "Loaded Children collection has unexpected size", 2, application.getChildren().size() );
        } );
    }

    // --- //

    @Entity
    @Table( name = "CHILD" )
    private static class Child {

        Long id;

        @Id
        @Column( name = "id", unique = true, nullable = false )
        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Table( name = "PARENT" )
    private static class Parent {

        Long id;
        Set<String> names;
        Set<Child> children;

        @Id
        @Column( name = "id", unique = true, nullable = false )
        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        @ElementCollection
        Set<String> getNames() {
            return Collections.unmodifiableSet( names );
        }

        void setNames(Set<String> secrets) {
            this.names = secrets;
        }

        @ManyToMany( fetch = FetchType.LAZY, targetEntity = Child.class )
        Set<Child> getChildren() {
            return Collections.unmodifiableSet( children );
        }

        void addChild(Child child) {
            if (children == null) {
                children = new HashSet<>();
            }
            children.add( child );
        }
    }
}