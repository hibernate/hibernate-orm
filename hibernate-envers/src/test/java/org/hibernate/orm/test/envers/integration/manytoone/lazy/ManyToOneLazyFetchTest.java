/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.lazy;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that proxies are resolved correctly by the ToOneIdMapper such that when the values
 * are inserted for the join columns, they're resolved correctly avoiding ClassCastException
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13760")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@EnversTest
@DomainModel(annotatedClasses = { Shipment.class, Address.class, AddressVersion.class, User.class, ChildUser.class })
@SessionFactory
public class ManyToOneLazyFetchTest {
	private Long shipmentId;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		this.shipmentId = scope.fromTransaction( session -> {
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

		scope.inTransaction( session -> {
			final Shipment shipment = session.get( Shipment.class, shipmentId );

			Hibernate.initialize( shipment.getOrigin() );
			Hibernate.initialize( shipment.getDestination() );
			shipment.setClosed( true );

			session.merge( shipment );
			session.flush();
		} );
	}

	@Test
	public void testRevisionHistory(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( Shipment.class, shipmentId ) );
		} );
	}
}
