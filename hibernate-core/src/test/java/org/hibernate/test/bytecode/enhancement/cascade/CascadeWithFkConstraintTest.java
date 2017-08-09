/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.cascade;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10252" )
@RunWith( BytecodeEnhancerRunner.class )
public class CascadeWithFkConstraintTest extends BaseCoreFunctionalTestCase {

    private String garageId, car1Id, car2Id;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Garage.class, Car.class};
    }

    @Before
    public void prepare() {
        // Create garage, add 2 cars to garage
        doInJPA( this::sessionFactory, em -> {

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
    public void test() {
        // Remove garage
        doInJPA( this::sessionFactory, em -> {
            Garage toRemoveGarage = em.find( Garage.class, garageId );
            em.remove( toRemoveGarage );
        } );

        // Check if there is no garage but cars are still present
        doInJPA( this::sessionFactory, em -> {
            Garage foundGarage = em.find( Garage.class, garageId );
            Assert.assertNull( foundGarage );

            Car foundCar1 = em.find( Car.class, car1Id );
            Assert.assertEquals( car1Id, foundCar1.id );

            Car foundCar2 = em.find( Car.class, car2Id );
            Assert.assertEquals( car2Id, foundCar2.id );
        } );
    }

    // --- //

    @Entity
    @Table( name = "GARAGE" )
    private static class Garage {

        @Id
        String id;

        @OneToMany
        @JoinColumn( name = "GARAGE_ID" )
        Set<Car> cars = new HashSet<>();

        Garage() {
            id = UUID.randomUUID().toString();
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
            id = UUID.randomUUID().toString();
        }
    }
}
