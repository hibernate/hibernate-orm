/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.merge;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class CompositeMergeTest extends BaseCoreFunctionalTestCase {

    private long entityId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{ParentEntity.class, Address.class, Country.class};
    }

    @Before
    public void prepare() {
        ParentEntity parent = new ParentEntity();
        parent.description = "desc";
        parent.address = new Address();
        parent.address.street = "Sesame street";
        parent.address.country = new Country();
        parent.address.country.name = "Suriname";
        parent.address.country.languages = Arrays.asList( "english", "spanish" );

        parent.lazyField = new byte[100];

        doInHibernate( this::sessionFactory, s -> {
            s.persist( parent );
        } );

        checkDirtyTracking( parent );
        entityId = parent.id;
    }

    @Test
    public void test() {
        ParentEntity[] parent = new ParentEntity[3];

        doInHibernate( this::sessionFactory, s -> {
            parent[0] = s.get( ParentEntity.class, entityId );
        } );

        checkDirtyTracking( parent[0] );

        parent[0].address.country.name = "Paraguai";

        checkDirtyTracking( parent[0], "address.country" );

        doInHibernate( this::sessionFactory, s -> {
            parent[1] = (ParentEntity) s.merge( parent[0] );
            checkDirtyTracking( parent[0], "address.country" );
            checkDirtyTracking( parent[1], "address.country" );
        } );

        checkDirtyTracking( parent[0], "address.country" );
        checkDirtyTracking( parent[1] );

        parent[1].address.country.name = "Honduras";

        checkDirtyTracking( parent[1], "address.country" );

        doInHibernate( this::sessionFactory, s -> {
            s.saveOrUpdate( parent[1] );
            checkDirtyTracking( parent[1], "address.country" );
        } );

        doInHibernate( this::sessionFactory, s -> {
            parent[2] = s.get( ParentEntity.class, entityId );
            Assert.assertEquals( "Honduras", parent[2].address.country.name );
        } );
    }

    // --- //

    @Entity
    @Table( name = "PARENT_ENTITY" )
    private static class ParentEntity {

        @Id
        @GeneratedValue
        Long id;

        String description;

        @Embedded
        Address address;

        @Basic( fetch = FetchType.LAZY )
        byte[] lazyField;
    }

    @Embeddable
    @Table( name = "ADDRESS" )
    private static class Address {

        String street;

        @Embedded
        Country country;
    }

    @Embeddable
    @Table( name = "COUNTRY" )
    private static class Country {

        String name;

        @ElementCollection
        @CollectionTable( name = "languages", joinColumns = @JoinColumn( name = "id", referencedColumnName = "id" ) )
        List<String> languages;
    }
}
