/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@JiraKey( "HHH-16774" )
@RunWith( BytecodeEnhancerRunner.class )
public class DirtyTrackingEmbeddableTest {

    @Test
    public void test() {
        SimpleEntity entity = new SimpleEntity();
        Address address = new Address();
        entity.address = address;
        EnhancerTestUtils.clearDirtyTracking( entity );

        // testing composite object
        address.city = "Arendal";
        EnhancerTestUtils.checkDirtyTracking( entity, "address" );
        EnhancerTestUtils.clearDirtyTracking( entity );
    }

    // --- //

    @Embeddable
    private static class Address {
        String street1;
        String street2;
        String city;
        String state;
        String zip;
        String phone;
    }

    @Entity
    private static class SimpleEntity {

        @Id
        Long id;

        String name;

        Address address;

    }
}
