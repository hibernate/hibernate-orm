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
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
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
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.getFieldByReflection;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class LazyLoadingTest extends BaseCoreFunctionalTestCase {

    private static final int CHILDREN_SIZE = 10;
    private Long parentID;
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
                Child child = new Child( "Child #" + i );
                child.parent = parent;
                parent.addChild( child );
                s.persist( child );
                lastChildID = child.id;
            }
            s.persist( parent );
            parentID = parent.id;
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            Child loadedChild = s.load( Child.class, lastChildID );

            Object nameByReflection = getFieldByReflection( loadedChild, "name" );
            assertNotNull( "Non-lazy field 'name' was not loaded", nameByReflection );

            Object parentByReflection = getFieldByReflection( loadedChild, "parent" );
            assertNull( "Lazy field 'parent' is initialized", parentByReflection );
            assertFalse( loadedChild instanceof HibernateProxy );

            Parent loadedParent = loadedChild.parent;
            assertThat( loadedChild.name, notNullValue() );
            assertThat( loadedParent, notNullValue() );
            assertThat( loadedChild.parent, notNullValue() );

            checkDirtyTracking( loadedChild );

            parentByReflection = getFieldByReflection( loadedChild, "parent" );
            Object childrenByReflection = getFieldByReflection( loadedParent, "children" );
            assertNotNull( "Lazy field 'parent' is not loaded", parentByReflection );
            assertNull( "Lazy field 'children' is initialized", childrenByReflection );
            assertFalse( loadedParent instanceof HibernateProxy );
            assertEquals( parentID, loadedParent.id );

            Collection<Child> loadedChildren = loadedParent.children;

            checkDirtyTracking( loadedChild );
            checkDirtyTracking( loadedParent );

            childrenByReflection = getFieldByReflection( loadedParent, "children" );
            assertNotNull( "Lazy field 'children' is not loaded", childrenByReflection );
            assertFalse( loadedChildren instanceof HibernateProxy );
            assertEquals( CHILDREN_SIZE, loadedChildren.size() );
            assertTrue( loadedChildren.contains( loadedChild ) );
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

        void addChild(Child child) {
            if ( children == null ) {
                children = new ArrayList<>();
            }
            children.add( child );
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
