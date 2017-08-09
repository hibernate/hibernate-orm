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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue( jiraKey = "HHH-10708" )
@RunWith( BytecodeEnhancerRunner.class )
public class UnexpectedDeleteTest2 extends BaseCoreFunctionalTestCase {

    private Bar myBar;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{Foo.class, Bar.class};
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Bar bar = new Bar();
            Foo foo1 = new Foo();
            Foo foo2 = new Foo();
            s.save( bar );
            s.save( foo1 );
            s.save( foo2 );

            bar.foos.add( foo1 );
            bar.foos.add( foo2 );

            myBar = bar;
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            s.refresh( myBar );
            Assert.assertFalse( myBar.foos.isEmpty() );

            // The issue is that currently, for some unknown reason, foos are deleted on flush
        } );

        doInHibernate( this::sessionFactory, s -> {
            Bar bar = s.get( Bar.class, myBar.id );
            Assert.assertFalse( bar.foos.isEmpty() );
        } );
    }

    // --- //

    @Entity
    @Table( name = "BAR" )
    private static class Bar {

        @Id
        @GeneratedValue
        Long id;

        @ManyToMany( fetch = FetchType.LAZY, targetEntity = Foo.class )
        Set<Foo> foos = new HashSet<>();
    }

    @Entity
    @Table( name = "FOO" )
    private static class Foo {

        @Id
        @GeneratedValue
        Long id;
    }
}