/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * Tests that proxies can still be resolved correctly in ToOneIdMapper even the object is already deleted and can't
 * find in cache. This can happen if the deleted object is an inherited object, and when the child object is deleted,
 * we cannot find the object with the parent class name anymore.
 *
 * @author Luke Chen
 */
@JiraKey(value = "HHH-13945")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class ManyToOneLazyDeleteTest extends BaseEnversFunctionalTestCase {
	private Long shipmentId;
	private User user;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Shipment.class, Address.class, AddressVersion.class, User.class, ChildUser.class };
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
			user = new ChildUser();
			session.persist( origin );
			session.persist( destination );
			session.persist( user );

			session.flush();
			shipment.setUser( user );
			shipment.setOrigin( originVersion0 );
			shipment.setDestination( destinationVersion0 );

			session.merge( shipment );
			session.flush();

			return shipment.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final Shipment shipment = session.get( Shipment.class, shipmentId );
			session.remove(shipment);
			// Cast the User instance to the ChildUser, and delete the child one, so the cache for
			// the User instance will not be there, and entityNotFound exception will be thrown while envers processing it
			ChildUser childUser = session.get(ChildUser.class, user.getId());
			session.remove(childUser);

			session.flush();
		} );
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );

		settings.put(EnversSettings.STORE_DATA_AT_DELETE, "true");
	}
}
