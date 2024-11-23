/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

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
            assertThat( loadedChild, not( instanceOf( HibernateProxy.class ) ) );
            assertThat( loadedChild, instanceOf( PersistentAttributeInterceptable.class ) );
            final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
            final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
            assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

            assertThat( Hibernate.isPropertyInitialized( loadedChild, "name" ), is( false ) );
            assertThat( Hibernate.isPropertyInitialized( loadedChild, "parent" ), is( false ) );
            assertThat( Hibernate.isPropertyInitialized( loadedChild, "children" ), is( false ) );

            Parent loadedParent = loadedChild.parent;
            assertThat( loadedChild.name, notNullValue() );
            assertThat( loadedParent, notNullValue() );
            assertThat( loadedChild.parent, notNullValue() );

            checkDirtyTracking( loadedChild );

            assertThat( Hibernate.isPropertyInitialized( loadedChild, "name" ), is( true ) );
            assertThat( Hibernate.isPropertyInitialized( loadedChild, "parent" ), is( true ) );
            assertThat( Hibernate.isPropertyInitialized( loadedChild, "children" ), is( true ) );

            Collection<Child> loadedChildren = loadedParent.children;
            assertThat( Hibernate.isInitialized( loadedChildren ), is( false ) );

            checkDirtyTracking( loadedChild );
            checkDirtyTracking( loadedParent );

            loadedChildren.size();
            assertThat( Hibernate.isInitialized( loadedChildren ), is( true ) );
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
