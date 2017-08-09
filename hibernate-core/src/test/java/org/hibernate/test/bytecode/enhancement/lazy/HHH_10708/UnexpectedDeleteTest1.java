/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue( jiraKey = "HHH-10708" )
@RunWith( BytecodeEnhancerRunner.class )
public class UnexpectedDeleteTest1 extends BaseCoreFunctionalTestCase {

    private long fooId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{Foo.class, Bar.class};
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Bar bar1 = new Bar();
            Bar bar2 = new Bar();
            Foo foo = new Foo();
            s.save( bar1 );
            s.save( bar2 );
            s.save( foo );
            bar1.foo = foo;
            bar2.foo = foo;
            fooId = foo.id;
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            Foo foo = s.get( Foo.class, fooId );

            // accessing the collection results in an exception
            foo.bars.size();
        } );
    }

    // --- //

    @Entity
    @Table( name = "BAR" )
    private static class Bar {

        @Id
        @GeneratedValue
        Long id;

        @ManyToOne
        @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
        Foo foo;
    }

    @Entity
    @Table( name = "FOO" )
    private static class Foo {

        @Id
        @GeneratedValue
        Long id;

        @OneToMany( orphanRemoval = true, mappedBy = "foo", targetEntity = Bar.class )
        @Cascade( CascadeType.ALL )
        Set<Bar> bars = new HashSet<>();
    }
}