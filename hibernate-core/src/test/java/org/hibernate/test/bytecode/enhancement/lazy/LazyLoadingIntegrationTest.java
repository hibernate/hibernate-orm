/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class LazyLoadingIntegrationTest extends BaseCoreFunctionalTestCase {

    private static final int CHILDREN_SIZE = 10;
    private Long lastChildID;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Parent.class, Child.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Parent parent = new Parent();
            for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
                Child child = new Child();
                // Association management should kick in here
                child.parent = parent;
                s.persist( child );
                lastChildID = child.id;
            }
            s.persist( parent );
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            Child loadedChild = s.load( Child.class, lastChildID );
            checkDirtyTracking( loadedChild );

            loadedChild.name = "Barrabas";
            checkDirtyTracking( loadedChild, "name" );

            Parent loadedParent = loadedChild.parent;
            checkDirtyTracking( loadedChild, "name" );
            checkDirtyTracking( loadedParent );

            List<Child> loadedChildren = new ArrayList<>( loadedParent.children );
            loadedChildren.remove( 0 );
            loadedChildren.remove( loadedChild );
            loadedParent.setChildren( loadedChildren );

            Assert.assertNull( loadedChild.parent );
        } );
    }

    // --- //

    @Entity
    @Table( name = "PARENT" )
    private static class Parent {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @OneToMany( mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        List<Child> children;

        void setChildren(List<Child> children) {
            this.children = children;
        }
    }

    @Entity
    @Table( name = "CHILD" )
    private static class Child {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        @LazyToOne( LazyToOneOption.NO_PROXY )
        Parent parent;

        String name;

        Child() {
        }

        Child(String name) {
            this.name = name;
        }
    }
}
