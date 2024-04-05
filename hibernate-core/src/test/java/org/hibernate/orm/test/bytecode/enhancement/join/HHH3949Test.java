/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.join;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;

import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey( "HHH-3949" )
@DomainModel(
        annotatedClasses = {
             HHH3949Test.Person.class, HHH3949Test.Vehicle.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
                @Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
                // see HHH-3949 for further details ^^^^^
        }
)
@SessionFactory
@BytecodeEnhanced
public class HHH3949Test {

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction( s -> {

            // it is important that the data associations remain as follows:
            //		* Johnny <-> Volkswagen Golf
            //		* Ricky <-> Subaru Impreza
            //		* Rosy -> none
            //		* none <- Renault Truck
            //
            // see #shouldHaveVehicle and #shouldHaveDriver

            Person person1 = new Person( "Johnny" );
            Person person2 = new Person( "Ricky" );
            Person person3 = new Person( "Rosy" );
            s.save( person1 );
            s.save( person2 );
            s.save( person3 );

            Vehicle vehicle1 = new Vehicle( "Volkswagen Golf" );
            vehicle1.setDriver( person1 );
            s.save( vehicle1 );

            Vehicle vehicle2 = new Vehicle( "Subaru Impreza" );
            vehicle2.setDriver( person2 );
            person2.setVehicle( vehicle2 );
            s.save( vehicle2 );

            Vehicle vehicle3 = new Vehicle( "Renault Truck" );
            s.save( vehicle3 );
        } );
    }

    @Test
    public void test1(SessionFactoryScope scope) {
        performQueryAndVerifyPersonResults( scope, "from Person p left join fetch p.vehicle" );
    }

    @Test
    public void test2(SessionFactoryScope scope) {
        performQueryAndVerifyVehicleResults( scope, "from Vehicle v left join fetch v.driver" );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void test3(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
            CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
            criteria.from( Person.class ).fetch( "vehicle", JoinType.LEFT );
            List<Person> persons = s.createQuery( criteria ).list();
//            List<Person> persons = (List<Person>) s.createCriteria( Person.class ).setFetchMode( "vehicle", FetchMode.JOIN ).list();
            for ( Person person : persons ) {
                if ( shouldHaveVehicle( person ) ) {
                    assertNotNull( person.getVehicle() );
                    assertNotNull( person.getVehicle().getDriver() );
                }
            }
        } );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void test4(SessionFactoryScope scope) {
        List<Vehicle> vehicles;

        try ( Session s = scope.getSessionFactory().openSession() ) {
            CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
            CriteriaQuery<Vehicle> criteria = criteriaBuilder.createQuery( Vehicle.class );
            criteria.from( Vehicle.class ).fetch( "driver", JoinType.LEFT );
            vehicles = s.createQuery( criteria ).list();
//            vehicles = (List<Vehicle>) s.createCriteria( Vehicle.class ).setFetchMode( "driver", FetchMode.JOIN ).list();
        }

        for ( Vehicle vehicle : vehicles ) {
            if ( shouldHaveDriver( vehicle ) ) {
                assertNotNull( vehicle.getDriver() );
                assertNotNull( vehicle.getDriver().getVehicle() );
            }
        }
    }

    // --- //

    @SuppressWarnings( "unchecked" )
    private void performQueryAndVerifyPersonResults(SessionFactoryScope scope, String query) {
        List<Person> persons;
        try ( Session s = scope.getSessionFactory().openSession() ) {
            persons = (List<Person>) s.createQuery( query ).list();
        }
        for ( Person person : persons ) {
            assertTrue( isInitialized( person ) );
            if ( shouldHaveVehicle( person ) ) {
                // We used a "join fetch", so the vehicle must be initialized
                // before we even call the getter
                // (which could trigger lazy initialization if the join fetch didn't work).
                assertTrue( isPropertyInitialized( person, "vehicle" ) );

                assertNotNull( person.getVehicle() );
                assertTrue( isInitialized( person.getVehicle() ) );
                assertNotNull( person.getVehicle().getDriver() );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void performQueryAndVerifyVehicleResults(SessionFactoryScope scope, String query) {
        List<Vehicle> vehicles;
        try ( Session s = scope.getSessionFactory().openSession() ) {
            vehicles = (List<Vehicle>) s.createQuery( query ).list();
        }
        for ( Vehicle vehicle : vehicles ) {
            if ( shouldHaveDriver( vehicle ) ) {
                // We used a "join fetch", so the drover must be initialized
                // before we even call the getter
                // (which could trigger lazy initialization if the join fetch didn't work).
                assertTrue( isPropertyInitialized( vehicle, "driver" ) );

                assertNotNull( vehicle.getDriver() );
                assertNotNull( vehicle.getDriver().getVehicle() );
            }
        }
    }

    private boolean shouldHaveVehicle(Person person) {
        return "Johnny".equals( person.name ) || "Ricky".equals( person.name );
    }

    private boolean shouldHaveDriver(Vehicle vehicle) {
        return "Volkswagen Golf".equals( vehicle.name ) || "Subaru Impreza".equals( vehicle.name );
    }

    // --- //

    @Entity( name = "Person" )
    @Table( name = "PERSON" )
    static class Person {

        @Id
        @GeneratedValue
        Long id;

        String name;

        @OneToOne( optional = true, mappedBy = "driver", fetch = FetchType.LAZY )
        @LazyToOne( LazyToOneOption.NO_PROXY )
        Vehicle vehicle;

        Person() {
        }

        Person(String name) {
            this.name = name;
        }

        Vehicle getVehicle() {
            return vehicle;
        }

        void setVehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
        }
    }

    @Entity( name = "Vehicle" )
    @Table( name = "VEHICLE" )
    static class Vehicle {

        @Id
        @GeneratedValue
        Long id;

        String name;

        @OneToOne( optional = true, fetch = FetchType.LAZY )
        Person driver;

        Vehicle() {
        }

        Vehicle(String name) {
            this.name = name;
        }

        Person getDriver() {
            return driver;
        }

        void setDriver(Person driver) {
            this.driver = driver;
        }
    }
}

