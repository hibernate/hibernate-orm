/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.getFieldByReflection;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-11155" )
@RunWith( BytecodeEnhancerRunner.class )
public class LazyGroupTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{Child.class, Parent.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Child c1 = new Child( "steve", "Hibernater" );
            Child c2 = new Child( "sally", "Joe Mama" );

            Parent p1 = new Parent( "Hibernate" );
            Parent p2 = new Parent( "Swimming" );

            c1.parent = p1;
            p1.children.add( c1 );

            c1.alternateParent = p2;
            p2.alternateChildren.add( c1 );

            c2.parent = p2;
            p2.children.add( c2 );

            c2.alternateParent = p1;
            p1.alternateChildren.add( c2 );

            s.save( p1 );
            s.save( p2 );
        } );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-10267" )
    public void testAccess() {
        doInHibernate( this::sessionFactory, s -> {
            Child c1 = (Child) s.createQuery( "from Child c where c.name = :name" ).setParameter( "name", "steve" ).uniqueResult();

            // verify the expected initial loaded state
            assertLoaded( c1, "name" );
            assertNotLoaded( c1, "nickName" );
            assertNotLoaded( c1, "parent" );
            assertNotLoaded( c1, "alternateParent" );

            // Now lets access nickName which ought to initialize nickName and parent, but not alternateParent
            c1.getNickName();
            assertLoaded( c1, "nickName" );
            assertLoaded( c1, "parent" );
            assertNotLoaded( c1, "alternateParent" );
            assertEquals( "Hibernate", c1.parent.nombre );
            assertFalse( c1.parent instanceof HibernateProxy );

            Child c2 = (Child) s.createQuery( "from Child c where c.name = :name" ).setParameter( "name", "sally" ).uniqueResult();

            // verify the expected initial loaded state
            assertLoaded( c2, "name" );
            assertNotLoaded( c2, "nickName" );
            assertNotLoaded( c2, "parent" );
            assertNotLoaded( c2, "alternateParent" );

            // Now lets access alternateParent which ought to initialize alternateParent and nothing else
            c2.getAlternateParent();
            assertNotLoaded( c2, "nickName" );
            assertNotLoaded( c2, "parent" );
            assertLoaded( c2, "alternateParent" );
            assertEquals( "Hibernate", c2.alternateParent.nombre );
            assertFalse( c2.alternateParent instanceof HibernateProxy );
        } );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-11155" )
    public void testUpdate() {
        Parent p1New = new Parent();
        p1New.nombre = "p1New";

        doInHibernate( this::sessionFactory, s -> {
            Child c1 = (Child) s.createQuery( "from Child c where c.name = :name" ).setParameter( "name", "steve" ).uniqueResult();

            // verify the expected initial loaded state
            assertLoaded( c1, "name" );
            assertNotLoaded( c1, "nickName" );
            assertNotLoaded( c1, "parent" );
            assertNotLoaded( c1, "alternateParent" );

            // Now lets update nickName which ought to initialize nickName and parent, but not alternateParent
            c1.nickName = "new nickName";
            assertLoaded( c1, "nickName" );
            assertNotLoaded( c1, "parent" );
            assertNotLoaded( c1, "alternateParent" );
            assertEquals( "Hibernate", c1.parent.nombre );
            assertFalse( c1.parent instanceof HibernateProxy );

            // Now update c1.parent
            c1.parent.children.remove( c1 );
            c1.parent = p1New;
            p1New.children.add( c1 );
        } );

        // verify updates
        doInHibernate( this::sessionFactory, s -> {
            Child c1 = (Child) s.createQuery( "from Child c where c.name = :name" ).setParameter( "name", "steve" ).uniqueResult();
            assertEquals( "new nickName", c1.getNickName() );
            assertEquals( "p1New", c1.parent.nombre );
            assertFalse( c1.parent instanceof HibernateProxy );
        } );

        doInHibernate( this::sessionFactory, s -> {
            Child c2 = (Child) s.createQuery( "from Child c where c.name = :name" ).setParameter( "name", "sally" ).uniqueResult();

            // verify the expected initial loaded state
            assertLoaded( c2, "name" );
            assertNotLoaded( c2, "nickName" );
            assertNotLoaded( c2, "parent" );
            assertNotLoaded( c2, "alternateParent" );

            // Now lets access and update alternateParent which ought to initialize alternateParent and nothing else
            Parent p1 = c2.getAlternateParent();
            c2.alternateParent = p1New;
            assertNotLoaded( c2, "nickName" );
            assertNotLoaded( c2, "parent" );
            assertLoaded( c2, "alternateParent" );
            assertEquals( "p1New", c2.getAlternateParent().nombre );
            assertFalse( c2.getAlternateParent() instanceof HibernateProxy );

            p1.alternateChildren.remove( c2 );
            p1New.alternateChildren.add( c2 );
        } );

        // verify update
        doInHibernate( this::sessionFactory, s -> {
            Child c2 = (Child) s.createQuery( "from Child c where c.name = :name" ).setParameter( "name", "sally" ).uniqueResult();
            assertEquals( "p1New", c2.getAlternateParent().nombre );
        } );
    }

    @After
    public void cleanup() {
        doInHibernate( this::sessionFactory, s -> {
            s.createQuery( "delete Child" ).executeUpdate();
            s.createQuery( "delete Parent" ).executeUpdate();
        } );
    }

    // --- //

    private void assertLoaded(Object owner, String name) {
        // NOTE we assume null == not-loaded
        Object fieldByReflection = getFieldByReflection( owner, name );
        assertNotNull( "Expecting field '" + name + "' to be loaded, but it was not", fieldByReflection );
    }

    private void assertNotLoaded(Object owner, String name) {
        // NOTE we assume null == not-loaded
        Object fieldByReflection = getFieldByReflection( owner, name );
        assertNull( "Expecting field '" + name + "' to be not loaded, but it was", fieldByReflection );
    }

    // --- //

    @Entity( name = "Parent" )
    @Table( name = "PARENT" )
    private static class Parent {
        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        String nombre;

        @OneToMany( mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        List<Child> children = new ArrayList<>();

        @OneToMany( mappedBy = "alternateParent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        List<Child> alternateChildren = new ArrayList<>();

        Parent() {
        }

        Parent(String nombre) {
            this.nombre = nombre;
        }
    }

    @Entity( name = "Child" )
    @Table( name = "CHILD" )
    private static class Child {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        String name;

        @Basic( fetch = FetchType.LAZY )
        String nickName;

        @ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        @LazyToOne( LazyToOneOption.NO_PROXY )
        Parent parent;

        @ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        @LazyToOne( LazyToOneOption.NO_PROXY )
        @LazyGroup( "SECONDARY" )
        Parent alternateParent;

        Child() {
        }

        Child(String name, String nickName) {
            this.name = name;
            this.nickName = nickName;
        }

        Parent getAlternateParent() {
            return alternateParent;
        }

        String getNickName() {
            return nickName;
        }
    }
}
