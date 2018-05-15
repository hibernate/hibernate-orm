/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.event.internal.DefaultFlushEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10922" )
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext( {EnhancerTestContext.class, LazyProxyOnEnhancedEntityTest.NoLazyLoadingContext.class} )
public class LazyProxyOnEnhancedEntityTest extends BaseCoreFunctionalTestCase {

    private Long parentID;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Parent.class, Child.class};
    }

    @Before
    public void prepare() {
        doInJPA( this::sessionFactory, em -> {
            Child c = new Child();
            em.persist( c );

            Parent parent = new Parent();
            parent.setChild( c );
            em.persist( parent );
            parentID = parent.getId();
        } );
    }

    @Test
    public void test() {
        EventListenerRegistry registry = sessionFactory().getServiceRegistry().getService( EventListenerRegistry.class );
        registry.prependListeners( EventType.LOAD, new ImmediateLoadTrap() );

        doInJPA( this::sessionFactory, em -> {

            em.find( Parent.class, parentID );

            // unwanted lazy load occurs on flush
        } );
    }

    private static class ImmediateLoadTrap implements LoadEventListener {
        @Override
        public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
            if ( IMMEDIATE_LOAD == loadType ) {
                String msg = loadType + ":" + event.getEntityClassName() + "#" + event.getEntityId();
                throw new RuntimeException( msg );
            }
        }
    }

    // --- //

    @Entity
    @Table( name = "PARENT" )
    private static class Parent {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @OneToOne( fetch = FetchType.LAZY
        )
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

        String name;

        Child() {
            // No-arg constructor necessary for proxy factory
        }
    }

    // --- //

    public static class NoLazyLoadingContext extends EnhancerTestContext {
        @Override
        public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
            return false;
        }
    }
}
