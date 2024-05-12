/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10252" )
@DomainModel(
        annotatedClasses = {
            CascadeWithFkConstraintTest.Garage.class, CascadeWithFkConstraintTest.Car.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class CascadeWithFkConstraintTest  {

    private String garageId, car1Id, car2Id;

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        // Create garage, add 2 cars to garage
        scope.inTransaction( em -> {

            Garage garage = new Garage();
            Car car1 = new Car();
            Car car2 = new Car();
            garage.insert( car1 );
            garage.insert( car2 );

            em.persist( garage );
            em.persist( car1 );
            em.persist( car2 );

            garageId = garage.id;
            car1Id = car1.id;
            car2Id = car2.id;
        } );
    }

    @Test
    public void test(SessionFactoryScope scope) {
        // Remove garage
        scope.inTransaction( em -> {
            Garage toRemoveGarage = em.find( Garage.class, garageId );
            em.remove( toRemoveGarage );
        } );

        // Check if there is no garage but cars are still present
        scope.inTransaction( em -> {
            Garage foundGarage = em.find( Garage.class, garageId );
            assertNull( foundGarage );

            Car foundCar1 = em.find( Car.class, car1Id );
            assertEquals( car1Id, foundCar1.id );

            Car foundCar2 = em.find( Car.class, car2Id );
            assertEquals( car2Id, foundCar2.id );
        } );
    }

    // --- //

    @Entity
    @Table( name = "GARAGE" )
    static class Garage {

        @Id
        String id;

        @OneToMany
        @JoinColumn( name = "GARAGE_ID" )
        Set<Car> cars = new HashSet<>();

        Garage() {
            id = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
        }

        void insert(Car aCar) {
            cars.add( aCar );
        }
    }

    @Entity
    @Table( name = "CAR" )
    public static class Car {

        @Id
        String id;

        Car() {
            id = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
        }
    }
}
