/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.cascade;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Luis Barreiro
 */
public class CascadeWithFkConstraintTestTask extends AbstractEnhancerTestTask {

    private String garageId, car1Id, car2Id;

    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Garage.class, Car.class};
    }

    public void prepare() {
        Configuration cfg = new Configuration();
        cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        super.prepare( cfg );

        // Create garage, add 2 cars to garage
        Session s = getFactory().openSession();
        Transaction tx = s.getTransaction();
        tx.begin();

        Garage garage = new Garage();
        Car car1 = new Car();
        Car car2 = new Car();
        garage.insert( car1 );
        garage.insert( car2 );

        s.persist( garage );
        s.persist( car1 );
        s.persist( car2 );

        tx.commit();
        s.close();

        garageId = garage.id;
        car1Id = car1.id;
        car2Id = car2.id;
    }

    public void execute() {

        // Remove garage

        Session s = getFactory().openSession();
        Transaction tx = s.getTransaction();
        tx.begin();

        Garage toRemoveGarage = s.get( Garage.class, garageId );
        s.delete( toRemoveGarage );

        tx.commit();
        s.close();

        // Check if there is no garage but cars are still present

        Session testSession = getFactory().openSession();
        tx = testSession.getTransaction();
        tx.begin();

        Garage foundGarage = testSession.get( Garage.class, garageId );
        Assert.assertNull( foundGarage );

        Car foundCar1 = testSession.get( Car.class, car1Id );
        Assert.assertEquals( car1Id, foundCar1.id );

        Car foundCar2 = testSession.get( Car.class, car2Id );
        Assert.assertEquals( car2Id, foundCar2.id );

        tx.commit();
        testSession.close();

    }

    protected void cleanup() {
    }

    // --- //

    @Entity(name="Garage")
    public static class Garage {

        @Id
        String id;

        @OneToMany
        @JoinColumn( name = "GARAGE_ID" )
        Set<Car> cars = new HashSet<>();

        public Garage() {
            this.id = UUID.randomUUID().toString();
        }

        public boolean insert(Car aCar) {
            return cars.add( aCar );
        }

    }

    @Entity(name="Car")
    public static class Car {

        @Id
        String id;

        public Car() {
            id = UUID.randomUUID().toString();
        }

    }

}
