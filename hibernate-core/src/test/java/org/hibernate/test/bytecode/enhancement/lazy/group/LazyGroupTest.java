/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import java.util.ArrayList;
import java.util.List;
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

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-11155" )
@RunWith( BytecodeEnhancerRunner.class )
public class LazyGroupTest extends BaseCoreFunctionalTestCase {
    private SQLStatementInterceptor sqlInterceptor;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{Child.class, Parent.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        sqlInterceptor = new SQLStatementInterceptor( configuration );
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
        sqlInterceptor.clear();

        inTransaction(
                (s) -> {
                    final Child c1 = s.createQuery( "from Child c where c.name = :name", Child.class )
                            .setParameter( "name", "steve" )
                            .uniqueResult();

                    assertThat( sqlInterceptor.getQueryCount(), is( 1 ) );

                    assertTrue( Hibernate.isPropertyInitialized( c1, "name" ) );

                    assertFalse( Hibernate.isPropertyInitialized( c1, "nickName" ) );

                    // parent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
                    assertThat( c1.getParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getParent() ) );

                    // alternateParent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
                    assertThat( c1.getAlternateParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );

                    // Now lets access nickName which ought to initialize nickName
                    c1.getNickName();
                    assertThat( sqlInterceptor.getQueryCount(), is( 2 ) );

                    assertTrue( Hibernate.isPropertyInitialized( c1, "nickName" ) );

                    // parent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
                    assertThat( c1.getParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getParent() ) );

                    // alternateParent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
                    assertThat( c1.getAlternateParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );


                    sqlInterceptor.clear();
                }
        );
    }

    @Test
    @TestForIssue( jiraKey = "HHH-11155" )
    public void testUpdate() {
        Parent p1New = new Parent();
        p1New.nombre = "p1New";

        inTransaction(
                (s) -> {
                    sqlInterceptor.clear();

                    final Child c1 = s.createQuery( "from Child c where c.name = :name", Child.class )
                            .setParameter( "name", "steve" )
                            .uniqueResult();
                    assertThat( sqlInterceptor.getQueryCount(), is( 1 ) );

                    assertTrue( Hibernate.isPropertyInitialized( c1, "name" ) );

                    assertFalse( Hibernate.isPropertyInitialized( c1, "nickName" ) );

                    // parent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
                    assertThat( c1.getParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getParent() ) );

                    // alternateParent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
                    assertThat( c1.getAlternateParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );

                    // Now lets update nickName
                    c1.nickName = "new nickName";

                    assertThat( sqlInterceptor.getQueryCount(), is( 1 ) );

                    assertTrue( Hibernate.isPropertyInitialized( c1, "name" ) );

                    assertTrue( Hibernate.isPropertyInitialized( c1, "nickName" ) );

                    // parent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
                    assertThat( c1.getParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getParent() ) );

                    // alternateParent should be an uninitialized enhanced-proxy
                    assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
                    assertThat( c1.getAlternateParent(), not( instanceOf( HibernateProxy.class ) ) );
                    assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );

                    // Now update c1.parent
                    c1.parent.children.remove( c1 );
                    c1.parent = p1New;
                    p1New.children.add( c1 );
                }
        );

        // verify updates
        inTransaction(
                (s) -> {
                    final Child c1 = s.createQuery( "from Child c where c.name = :name", Child.class )
                            .setParameter( "name", "steve" )
                            .uniqueResult();

                    assertThat( c1.getNickName(), is( "new nickName" ) );
                    assertThat( c1.parent.nombre, is( "p1New" ) );
                }
        );
    }

    @After
    public void cleanup() {
        doInHibernate( this::sessionFactory, s -> {
            s.createQuery( "delete Child" ).executeUpdate();
            s.createQuery( "delete Parent" ).executeUpdate();
        } );
    }

    // --- //

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

        public Parent getParent() {
            return parent;
        }

        Parent getAlternateParent() {
            return alternateParent;
        }

        String getNickName() {
            return nickName;
        }
    }
}
