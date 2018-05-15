/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class DirtyTrackingTest {

    @Test
    public void test() {
        SimpleEntity entity = new SimpleEntity();
        EnhancerTestUtils.clearDirtyTracking( entity );

        // Basic single field
        Long number = entity.getSomeNumber();
        EnhancerTestUtils.checkDirtyTracking( entity );
        entity.setSomeNumber( number + 1L );
        EnhancerTestUtils.checkDirtyTracking( entity, "someNumber" );
        EnhancerTestUtils.clearDirtyTracking( entity );
        entity.setSomeNumber( entity.getSomeNumber() );
        EnhancerTestUtils.checkDirtyTracking( entity );

        // Basic multi-field (Id properties are not flagged as dirty)
        entity.id = 2L;
        entity.active = !entity.active;
        entity.someNumber = 193L;
        EnhancerTestUtils.checkDirtyTracking( entity, "active", "someNumber" );
        EnhancerTestUtils.clearDirtyTracking( entity );

        // Setting the same value should not make it dirty
        entity.someNumber = 193L;
        EnhancerTestUtils.checkDirtyTracking( entity );

        // Collection
        List<String> stringList = new ArrayList<>();
        stringList.add( "FooBar" );
        entity.someStrings = stringList;
        EnhancerTestUtils.checkDirtyTracking( entity, "someStrings" );
        EnhancerTestUtils.clearDirtyTracking( entity );

        stringList.add( "BarFoo" );
        EnhancerTestUtils.checkDirtyTracking( entity, "someStrings" );
        EnhancerTestUtils.clearDirtyTracking( entity );

        // Association: this should not set the entity to dirty
        Set<Integer> intSet = new HashSet<>();
        intSet.add( 42 );
        entity.someInts = intSet;
        EnhancerTestUtils.checkDirtyTracking( entity );

        // testing composite object
        Address address = new Address();
        entity.address = address;
        address.city = "Arendal";
        EnhancerTestUtils.checkDirtyTracking( entity, "address" );
        EnhancerTestUtils.clearDirtyTracking( entity );

        // make sure that new composite instances are cleared
        Address address2 = new Address();
        entity.address = address2;
        address.street1 = "Heggedalveien";
        EnhancerTestUtils.checkDirtyTracking( entity, "address" );

        Country country = new Country();
        address2.country = country;
        country.name = "Norway";
        EnhancerTestUtils.checkDirtyTracking( entity, "address", "address.country" );

        address.country = null;
        entity.address = null;
    }

    // --- //

    @Embeddable
    private static class Address {
        @Embedded
        Country country;
        String street1;
        String street2;
        String city;
        String state;
        String zip;
        String phone;
    }

    @Embeddable
    private static class Country {
        String name;
    }

    @Entity
    private static class SimpleEntity {

        @Id
        Long id;

        String name;

        Boolean active = Boolean.FALSE;

        Long someNumber = 0L;

        List<String> someStrings;

        @OneToMany
        Set<Integer> someInts;

        @Embedded
        Address address;

        @Embedded
        Address address2;

        public Long getSomeNumber() {
            return someNumber;
        }

        public void setSomeNumber(Long someNumber) {
            this.someNumber = someNumber;
        }
    }
}