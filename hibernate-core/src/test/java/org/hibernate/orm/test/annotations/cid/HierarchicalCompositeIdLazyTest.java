package org.hibernate.orm.test.annotations.cid;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
        annotatedClasses = {
                Flight.class,
                FlightSegment.class,
                FlightSegmentConfiguration.class,
                Freight.class
        }
)
public class HierarchicalCompositeIdLazyTest {

    @Test
    void testFetchFlightSegmentFromFlight(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    FlightSegment flightSegment = new FlightSegment();
                    flightSegment.setSegmentNumber(1);

                    Flight flight = new Flight();
                    flight.setId(1);
                    flight.setSegments(new ArrayList<>());
                    flight.addSegment(flightSegment);

                    entityManager.persist(flight);
                    entityManager.flush();
                    entityManager.clear();
                }
        );

        scope.inTransaction(
                entityManager -> {
                    Flight flight = entityManager.find(Flight.class, 1);
                    assertNotNull(flight);
                    assertEquals(1, flight.getSegments().get(0).getSegmentNumber());
                }
        );
    }

    @Test
    void testFetchFlightFromFlightSegment(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    FlightSegment flightSegment = new FlightSegment();
                    flightSegment.setSegmentNumber(1);

                    Flight flight = new Flight();
                    flight.setId(1);
                    flight.setSegments(new ArrayList<>());
                    flight.addSegment(flightSegment);

                    entityManager.persist(flight);
                    entityManager.flush();
                    entityManager.clear();
                }
        );

        scope.inTransaction(
                entityManager -> {
                    FlightSegment segment = entityManager.find(FlightSegment.class, new FlightSegmentId(1, 1));
                    assertNotNull(segment);
                    assertEquals(1, segment.getFlight().getId());
                }
        );
    }

    @Test
    void testFetchFlightFromFlightSegmentConfiguration(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    FlightSegmentConfiguration flightSegmentConfiguration = new FlightSegmentConfiguration();

                    FlightSegment flightSegment = new FlightSegment();
                    flightSegment.setSegmentNumber(1);
                    flightSegment.setConfiguration(flightSegmentConfiguration);

                    Flight flight = new Flight();
                    flight.setId(1);
                    flight.setSegments(new ArrayList<>());
                    flight.addSegment(flightSegment);

                    entityManager.persist(flight);
                    entityManager.flush();
                    entityManager.clear();
                }
        );

        scope.inTransaction(
                entityManager -> {
                    FlightSegmentConfigurationId id = new FlightSegmentConfigurationId(new FlightSegmentId(1, 1));
                    FlightSegmentConfiguration configuration = entityManager.find(FlightSegmentConfiguration.class, id);
                    assertNotNull(configuration);
                    assertEquals(1, configuration.getSegment().getFlight().getId());
                    assertNotNull(configuration.getSegment().getConfiguration());
                }
        );
    }

    @Test
    void testFetchFlightFromFlightSegmentConfigurationViaQuery(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    FlightSegmentConfiguration flightSegmentConfiguration = new FlightSegmentConfiguration();

                    FlightSegment flightSegment = new FlightSegment();
                    flightSegment.setSegmentNumber(1);
                    flightSegment.setConfiguration(flightSegmentConfiguration);

                    Flight flight = new Flight();
                    flight.setId(1);
                    flight.setSegments(new ArrayList<>());
                    flight.addSegment(flightSegment);

                    entityManager.persist(flight);
                    entityManager.flush();
                    entityManager.clear();
                }
        );

        scope.inTransaction(
                entityManager -> {
                    FlightSegmentConfigurationId id = new FlightSegmentConfigurationId(new FlightSegmentId(1, 1));
                    FlightSegmentConfiguration configuration = entityManager
                            .createQuery("from FlightSegmentConfiguration where id = :id", FlightSegmentConfiguration.class)
                            .setParameter("id", id)
                            .getSingleResult();
                    assertNotNull(configuration);
                    assertEquals(1, configuration.getSegment().getFlight().getId());
                    assertNotNull(configuration.getSegment().getConfiguration());
                }
        );
    }

    @Test
    void testFetchFlightFromFreight(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    FlightSegment flightSegment = new FlightSegment();
                    flightSegment.setSegmentNumber(1);

                    Flight flight = new Flight();
                    flight.setId(1);
                    flight.setSegments(new ArrayList<>());
                    flight.addSegment(flightSegment);

                    entityManager.persist(flight);

                    Freight freight = new Freight();
                    freight.setFreightNumber(1);
                    freight.setFlightSegment(flightSegment);

                    entityManager.persist(freight);

                    entityManager.flush();
                    entityManager.clear();
                }
        );

        scope.inTransaction(
                entityManager -> {
                    Freight freight = entityManager.find(Freight.class, 1);
                    assertNotNull(freight);
                    assertEquals(1, freight.getFlightSegment().getFlight().getId());
                }
        );
    }

    @AfterEach
    void cleanUp(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    entityManager.createQuery("delete from Freight").executeUpdate();
                    entityManager.createQuery("delete from FlightSegmentConfiguration").executeUpdate();
                    entityManager.createQuery("delete from FlightSegment").executeUpdate();
                    entityManager.createQuery("delete from Flight").executeUpdate();
                }
        );
    }

}
