/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.fail;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-11459" )
@RunWith( BytecodeEnhancerRunner.class )
public class MergeEnhancedEntityDynamicUpdateTest extends BaseCoreFunctionalTestCase {
    private Person person;
    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Person.class, PersonAddress.class, NullablePerson.class};
    }

    @Before
    public void prepare() {
        person = new Person( 1L, "Sam" );
        doInHibernate( this::sessionFactory, s -> {
            s.persist( person );
        } );
    }

    @Test
    public void testMerge() {
        doInHibernate( this::sessionFactory, s -> {
            Person entity = s.find( Person.class, 1L );
            entity.name = "John";
            try {
                s.merge( entity );
            } catch ( RuntimeException e ) {
                fail( "Enhanced entity can't be merged: " + e.getMessage() );
            }
        } );
    }

    @Test
    public void testRefresh() {
        doInHibernate( this::sessionFactory, s -> {
            Person entity = s.find( Person.class, 1L );
            entity.name = "John";

            s.refresh( entity );

//            try {
//                s.refresh( entity );
//            } catch ( RuntimeException e ) {
//                fail( "Enhanced entity can't be refreshed: " + e.getMessage() );
//            }
        } );
    }

    @Test
    public void testMergeWithNullValues() {
        doInHibernate( this::sessionFactory, em -> {
            NullablePerson nullablePerson = new NullablePerson( 1L, "Sam", 100 );
            em.persist( nullablePerson );
        } );
        doInHibernate( this::sessionFactory, em -> {
            NullablePerson updated = em.find( NullablePerson.class, 1L );
            assertThat( updated.name ).isEqualTo( "Sam" );
            assertThat( updated.number ).isEqualTo( 100 );
        } );

        // only some properties are null
        doInHibernate( this::sessionFactory, em -> {
            NullablePerson nullablePerson = new NullablePerson( 1L, "Joe", null );
            em.merge( nullablePerson );
        } );
        doInHibernate( this::sessionFactory, em -> {
            NullablePerson updated = em.find( NullablePerson.class, 1L );
            assertThat( updated.name ).isEqualTo( "Joe" );
            assertThat( updated.number ).isNull();
        } );

        // all properties are null:
        doInHibernate( this::sessionFactory, em -> {
            NullablePerson nullablePerson = new NullablePerson( 1L, null, null );
            em.merge( nullablePerson );
        } );
        doInHibernate( this::sessionFactory, em -> {
            NullablePerson updated = em.find( NullablePerson.class, 1L );
            assertThat( updated.name ).isNull();
            assertThat( updated.number ).isNull();
        } );
    }

    @After
    public void cleanup() {
        doInHibernate( this::sessionFactory, s -> {
            s.delete( person );
        } );
        doInHibernate( this::sessionFactory, s -> {
            s.createQuery( "delete from NullablePerson" );
        } );
    }

    // --- //

    @Entity
    @Table( name = "PERSON" )
    @DynamicUpdate
    @DynamicInsert
    private static class Person {

        @Id
        Long id;

        @Column( name = "name", length = 10, nullable = false )
        String name;

        @OneToMany( fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true, cascade = CascadeType.ALL )
        List<PersonAddress> details = new ArrayList<>();

        Person() {
        }

        Person(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity
    @Table( name = "PERSON_ADDRESS" )
    @DynamicUpdate
    @DynamicInsert
    private static class PersonAddress {

        @Id
        Long id;

        @ManyToOne( optional = false, fetch = FetchType.LAZY )
        Person parent;
    }

    @Entity(name = "NullablePerson")
    @Table(name = "NULLABLE_PERSON")
    @DynamicUpdate
    @DynamicInsert
    private static class NullablePerson {

        @Id
        Long id;

        @Column
        String name;

        @Column(name = "NUMBER_COLUMN")
        Integer number;

        NullablePerson() {
        }

        NullablePerson(Long id, String name, Integer number) {
            this.id = id;
            this.name = name;
            this.number = number;
        }
    }
}
