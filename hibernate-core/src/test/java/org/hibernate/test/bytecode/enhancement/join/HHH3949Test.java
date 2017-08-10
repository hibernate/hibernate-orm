package org.hibernate.test.bytecode.enhancement.join;

import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;

import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-3949" )
@RunWith( BytecodeEnhancerRunner.class )
public class HHH3949Test extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Person.class, Vehicle.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        // see HHH-3949 for further details ^^^^^
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {

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
    public void test1() {
        // verify the work around query
        performQueryAndVerifyPersonResults( "from Person p fetch all properties left join fetch p.vehicle" );
        performQueryAndVerifyPersonResults( "from Person p left join fetch p.vehicle" );
    }

    @Test
    public void test2() {
        performQueryAndVerifyVehicleResults( "from Vehicle v fetch all properties left join fetch v.driver" );
        performQueryAndVerifyVehicleResults( "from Vehicle v left join fetch v.driver" );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void test3() {
        doInHibernate( this::sessionFactory, s -> {
            List<Person> persons = (List<Person>) s.createCriteria( Person.class ).setFetchMode( "vehicle", FetchMode.JOIN ).list();
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
    public void test4() {
        List<Vehicle> vehicles;

        try ( Session s = openSession() ) {
            vehicles = (List<Vehicle>) s.createCriteria( Vehicle.class ).setFetchMode( "driver", FetchMode.JOIN ).list();
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
    private void performQueryAndVerifyPersonResults(String query) {
        List<Person> persons;
        try ( Session s = openSession() ) {
            persons = (List<Person>) s.createQuery( query ).list();
        }
        for ( Person person : persons ) {
            assertTrue( isInitialized( person ) );
            if ( shouldHaveVehicle( person ) ) {
                assertNotNull( person.getVehicle() );
                assertTrue( isInitialized( person.getVehicle() ) );
                assertNotNull( person.getVehicle().getDriver() );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void performQueryAndVerifyVehicleResults(String query) {
        List<Vehicle> vehicles;
        try ( Session s = openSession() ) {
            vehicles = (List<Vehicle>) s.createQuery( query ).list();
        }
        for ( Vehicle vehicle : vehicles ) {
            if ( shouldHaveDriver( vehicle ) ) {
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
    private static class Person {

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
    private static class Vehicle {

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

