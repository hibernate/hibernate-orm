/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.HHH_10708;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@JiraKey( "HHH-10708" )
@DomainModel(
        annotatedClasses = {
               UnexpectedDeleteTest1.Foo.class, UnexpectedDeleteTest1.Bar.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class UnexpectedDeleteTest1 {

    private long fooId;

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
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
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            Foo foo = s.get( Foo.class, fooId );

            // accessing the collection results in an exception
            foo.bars.size();
        } );
    }

    // --- //

    @Entity(name = "Bar")
    @Table( name = "BAR" )
    static class Bar {

        @Id
        @GeneratedValue
        Long id;

        @ManyToOne
        @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
        Foo foo;
    }

    @Entity(name = "Foo")
    @Table( name = "FOO" )
    static class Foo {

        @Id
        @GeneratedValue
        Long id;

        @OneToMany( orphanRemoval = true, mappedBy = "foo", targetEntity = Bar.class )
        @Cascade( CascadeType.ALL )
        Set<Bar> bars = new HashSet<>();
    }
}
