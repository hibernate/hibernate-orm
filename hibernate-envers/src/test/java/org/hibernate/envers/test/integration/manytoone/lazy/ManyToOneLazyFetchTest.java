/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.hibernate.Hibernate;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * Tests that proxies are resolved correctly by the ToOneIdMapper such that when the values
 * are inserted for the join columns, they're resolved correclty avoiding ClassCastException
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-13760")
public class ManyToOneLazyFetchTest extends BaseEnversFunctionalTestCase {
    private Long shipmentId;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Shipment.class, Address.class, AddressVersion.class };
    }

    @Test
    @Priority(10)
    public void initData() {
        this.shipmentId = doInHibernate( this::sessionFactory, session -> {
            final Shipment shipment = new Shipment( Instant.now(), "system", Instant.now().plus( Duration.ofDays( 3 ) ), "abcd123", null, null );
            session.persist( shipment );
            session.flush();

            final Address origin = new Address( Instant.now(), "system", "Valencia#1" );
            final Address destination = new Address( Instant.now(), "system", "Madrid#3" );
            final AddressVersion originVersion0 = origin.addInitialVersion( "Poligono Manises" );
            final AddressVersion destinationVersion0 = destination.addInitialVersion( "Poligono Alcobendas" );
            session.persist( origin );
            session.persist( destination );
            session.flush();

            shipment.setOrigin( originVersion0 );
            shipment.setDestination( destinationVersion0 );
            session.merge( shipment );
            session.flush();

            return shipment.getId();
        } );

        doInHibernate( this::sessionFactory, session -> {
            final Shipment shipment = session.get( Shipment.class, shipmentId );

            Hibernate.initialize( shipment.getOrigin() );
            Hibernate.initialize( shipment.getDestination() );
            shipment.setClosed( true );

            session.merge( shipment );
            session.flush();
        } );
    }

    @Test
    public void testRevisionHistory() {
        assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( Shipment.class, shipmentId ) );
    }
}
