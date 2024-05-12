/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Luis Barreiro
 */
@DomainModel(
        annotatedClasses = {
            CompositeMergeTest.ParentEntity.class, CompositeMergeTest.Address.class, CompositeMergeTest.Country.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class CompositeMergeTest {

    private long entityId;

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        ParentEntity parent = new ParentEntity();
        parent.description = "desc";
        parent.address = new Address();
        parent.address.street = "Sesame street";
        parent.address.country = new Country();
        parent.address.country.name = "Suriname";
        parent.address.country.languages = Arrays.asList( "english", "spanish" );

        parent.lazyField = new byte[100];

        scope.inTransaction( s -> {
            s.persist( parent );
        } );

        checkDirtyTracking( parent );
        entityId = parent.id;
    }

    @Test
    public void test(SessionFactoryScope scope) {
        ParentEntity[] parent = new ParentEntity[3];

        scope.inTransaction( s -> {
            parent[0] = s.get( ParentEntity.class, entityId );
        } );

        checkDirtyTracking( parent[0] );

        parent[0].address.country.name = "Paraguai";

        checkDirtyTracking( parent[0], "address.country" );

        scope.inTransaction( s -> {
            parent[1] = (ParentEntity) s.merge( parent[0] );
            checkDirtyTracking( parent[0], "address.country" );
            checkDirtyTracking( parent[1], "address.country" );
        } );

        checkDirtyTracking( parent[0], "address.country" );
        checkDirtyTracking( parent[1] );

        parent[1].address.country.name = "Honduras";

        checkDirtyTracking( parent[1], "address.country" );

        scope.inTransaction( s -> {
            s.saveOrUpdate( parent[1] );
            checkDirtyTracking( parent[1], "address.country" );
        } );

        scope.inTransaction( s -> {
            parent[2] = s.get( ParentEntity.class, entityId );
            assertEquals( "Honduras", parent[2].address.country.name );
        } );
    }

    // --- //

    @Entity(name = "Parent")
    @Table( name = "PARENT_ENTITY" )
    static class ParentEntity {

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
    static class Address {

        String street;

        @Embedded
        Country country;
    }

    @Embeddable
    @Table( name = "COUNTRY" )
    static class Country {

        String name;

        @ElementCollection
        @CollectionTable( name = "languages", joinColumns = @JoinColumn( name = "id", referencedColumnName = "id" ) )
        List<String> languages;
    }
}
