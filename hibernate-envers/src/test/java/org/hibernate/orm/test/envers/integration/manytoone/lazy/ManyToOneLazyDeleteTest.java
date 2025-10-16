/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.lazy;

import java.time.Duration;
import java.time.Instant;

import org.hibernate.envers.configuration.EnversSettings;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests that proxies can still be resolved correctly in ToOneIdMapper even the object is already deleted and can't
 * find in cache. This can happen if the deleted object is an inherited object, and when the child object is deleted,
 * we cannot find the object with the parent class name anymore.
 *
 * @author Luke Chen
 */
@JiraKey(value = "HHH-13945")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@EnversTest
@DomainModel(annotatedClasses = { Shipment.class, Address.class, AddressVersion.class, User.class, ChildUser.class })
@ServiceRegistry(settings = @Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true"))
@SessionFactory
public class ManyToOneLazyDeleteTest {
	private Long shipmentId;
	private Long userId;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		this.shipmentId = scope.fromTransaction( session -> {
			final Shipment shipment = new Shipment(
					Instant.now(),
					"system",
					Instant.now().plus( Duration.ofDays( 3 ) ),
					"abcd123",
					null,
					null
			);
			session.persist( shipment );
			session.flush();

			final Address origin = new Address( Instant.now(), "system", "Valencia#1" );
			final Address destination = new Address( Instant.now(), "system", "Madrid#3" );
			final AddressVersion originVersion0 = origin.addInitialVersion( "Poligono Manises" );
			final AddressVersion destinationVersion0 = destination.addInitialVersion( "Poligono Alcobendas" );
			User user = new ChildUser();
			session.persist( origin );
			session.persist( destination );
			session.persist( user );

			session.flush();
			shipment.setUser( user );
			shipment.setOrigin( originVersion0 );
			shipment.setDestination( destinationVersion0 );

			session.merge( shipment );
			session.flush();

			this.userId = user.getId();
			return shipment.getId();
		} );

		scope.inTransaction( session -> {
			final Shipment shipment = session.get( Shipment.class, shipmentId );
			session.remove( shipment );
			// Cast the User instance to the ChildUser, and delete the child one, so the cache for
			// the User instance will not be there, and entityNotFound exception will be thrown while envers processing it
			ChildUser childUser = session.get( ChildUser.class, userId );
			session.remove( childUser );

			session.flush();
		} );
	}
}
