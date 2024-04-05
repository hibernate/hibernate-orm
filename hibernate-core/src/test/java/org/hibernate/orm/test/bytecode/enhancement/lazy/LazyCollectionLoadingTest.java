/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test for lazy collection handling in the new bytecode support.
 * Prior to HHH-10055 lazy collections were simply not handled.  The tests
 * initially added for HHH-10055 cover the more complicated case of handling
 * lazy collection initialization outside of a transaction; that is a bigger
 * fix, and I first want to get collection handling to work here in general.
 *
 * @author Steve Ebersole
 */
@JiraKey( "HHH-10055" )
@DomainModel(
        annotatedClasses = {
                LazyCollectionLoadingTest.Parent.class, LazyCollectionLoadingTest.Child.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
                @Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
        }
)
@SessionFactory
@BytecodeEnhanced
public class LazyCollectionLoadingTest {
    private static final int CHILDREN_SIZE = 10;
    private Long parentID;
    private Parent parent;

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            Parent parent = new Parent();
            parent.setChildren( new ArrayList<>() );
            for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
                Child child = new Child();
                child.parent = parent;
                s.persist( child );
            }
            s.persist( parent );
            parentID = parent.id;
        } );
    }

    @Test
    public void testTransaction(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            Parent parent = s.load( Parent.class, parentID );
            assertThat( parent, notNullValue() );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
            assertFalse( isPropertyInitialized( parent, "children" ) );
            checkDirtyTracking( parent );

            List children1 = parent.children;
            List children2 = parent.children;

            assertTrue( isPropertyInitialized( parent, "children" ) );
            checkDirtyTracking( parent );

            assertThat( children1, sameInstance( children2 ) );

            assertFalse( isInitialized( children1 ) );
            assertThat( children1.size(), equalTo( CHILDREN_SIZE ) );
            assertTrue( isInitialized( children1 ) );
        } );
    }

    @Test
    @JiraKey( "HHH-14620" )
    public void testTransaction_noProxy(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            // find will not return a proxy, which is exactly what we want here.
            Parent parent = s.find( Parent.class, parentID );
            assertThat( parent, notNullValue() );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
            checkDirtyTracking( parent );

            List<Child> children1 = parent.children;
            List<Child> children2 = parent.children;

            checkDirtyTracking( parent );

            assertThat( children1, sameInstance( children2 ) );

            // This check is important: a bug used to cause the collection to be initialized
            // during the call to parent.children above.
            // Note the same problem would occur if we were using getters:
            // we only need extended enhancement to be enabled.
            assertFalse( isInitialized( children1 ) );
            assertThat( children1.size(), equalTo( CHILDREN_SIZE ) );
            assertTrue( isInitialized( children1 ) );
        } );
    }

    @Test
    public void testNoTransaction(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            parent = s.load( Parent.class, parentID );
            assertThat( parent, notNullValue() );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
            assertFalse( isPropertyInitialized( parent, "children" ) );
        } );

        List children1 = parent.children;
        List children2 = parent.children;

        assertTrue( isPropertyInitialized( parent, "children" ) );

        checkDirtyTracking( parent );
        assertThat( children1, sameInstance( children2 ) );

        assertFalse( isInitialized( children1 ) );
        assertThat( children1.size(), equalTo( CHILDREN_SIZE ) );
        assertTrue( isInitialized( children1 ) );
    }

    // --- //

    @Entity
    @Table( name = "PARENT" )
    static class Parent {

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
    static class Child {

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
