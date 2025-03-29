/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.SerializationHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * This integration test verifies that serialized entities
 * can be deserialized and re-connected to a SessionFactory
 * by using the SessionFactory name rather than its UUID.
 * This is relevant for clustering, as different SessionFactory
 * instances on different nodes will have a different UUID
 * but can be configured to match the name.
 *
 * @author Marcel Overdijk
 */
public class MultipleSessionFactoriesProxyTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "proxy/DataPoint.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.SESSION_FACTORY_NAME, "sf-name" ); // explicitly define the session factory name
		cfg.setProperty( Environment.SESSION_FACTORY_NAME_IS_JNDI, false ); // do not bind it to jndi
	}

	@Test
	@JiraKey(value = "HHH-17172")
	public void testProxySerializationWithMultipleSessionFactories() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Container container = new Container( "container" );
		container.setOwner( new Owner( "owner" ) );
		container.setInfo( new Info( "blah blah blah" ) );
		container.getDataPoints().add( new DataPoint( new BigDecimal( 1 ), new BigDecimal( 1 ), "first data point" ) );
		container.getDataPoints().add( new DataPoint( new BigDecimal( 2 ), new BigDecimal( 2 ), "second data point" ) );
		s.persist( container );
		s.flush();
		s.clear();

		container = s.getReference( Container.class, container.getId() );
		assertFalse( Hibernate.isInitialized( container ) );
		container.getId();
		assertFalse( Hibernate.isInitialized( container ) );
		container.getName();
		assertTrue( Hibernate.isInitialized( container ) );

		t.commit();
		s.close();

		// Serialize the container.
		byte[] bytes = SerializationHelper.serialize( container );

		// Now deserialize, which works at this stage, because the session factory UUID in the
		// serialized object is the same as the current session factory.
		SerializationHelper.deserialize( bytes );

		// Now rebuild the session factory (it will get a new unique UUID).
		// As configured for this test an explicit session factory name is used ("sf-name"),
		// so we would expect the deserialization to work after rebuilding the session factory.
		// Rebuilding the session factory simulates multiple JVM instances with different session
		// factories (different UUID's, but same name!) trying to deserialize objects from a
		// centralized cache (e.g. Hazelcast).
		rebuildSessionFactory();

		// And this should be possible now: even though we lack of a matching session factory by
		// UUID, it should seek a match by using the session factory name as valid alternative.
		SerializationHelper.deserialize( bytes );

	}
}
