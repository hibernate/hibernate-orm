/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.serialization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.map.MapProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Selaron
 */
public class MapProxySerializationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "serialization/DynamicMapMappings.hbm.xml" };
	}

	@Override
	protected void configure(final Configuration configuration) {
		// enable LL without TX, which used to cause problems when serializing proxies (see HHH-12720)
		configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, Boolean.TRUE.toString() );

		// dynamic-map by default.
		configuration.setProperty( AvailableSettings.DEFAULT_ENTITY_MODE, EntityMode.MAP.getExternalName() );
	}

	@Before
	public void prepare() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();

		try {
			final Number count = (Number) s.createQuery("SELECT count(ID) FROM SimpleEntity").getSingleResult();
			if (count.longValue() > 0L) {
				// entity already added previously
				return;
			}

			final Map<String, Object> entity = new HashMap<>();
			entity.put( "id", 1L );
			entity.put( "name", "TheParent" );

			final Map<String, Object> c1 = new HashMap<>();
			c1.put( "id", 1L );
			c1.put( "parent", entity );

			s.save( "SimpleEntity", entity );
			s.save( "ChildEntity", c1 );
		}
		finally {
			t.commit();
			s.close();
		}
	}

	/**
	 * Tests that serializing an initialized proxy will serialize the target instead.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-7686")
	public void testInitializedProxySerializationIfTargetInPersistenceContext() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final Map<String, Object> child = (Map<String, Object>) s.load( "ChildEntity", 1L );

			final Map<String, Object> parent = (Map<String, Object>) child.get( "parent" );

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof MapProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Initialize the proxy
			parent.get( "name" );
			assertTrue( Hibernate.isInitialized( parent ) );

			// serialize/deserialize the proxy
			final Map<String, Object> deserializedParent =
					(Map<String, Object>) SerializationHelper.clone( (Serializable) parent );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.get( "name" ) );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	/**
	 * Tests that serializing a proxy which is not initialized
	 * but whose target has been (separately) added to the persistence context
	 * will serialized the target instead.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-7686")
	public void testUninitializedProxySerializationIfTargetInPersistenceContext() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final Map<String, Object> child = (Map<String, Object>) s.load( "ChildEntity", 1L );

			final Map<String, Object> parent = (Map<String, Object>) child.get( "parent" );

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof MapProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Load the target of the proxy without the proxy being made aware of it
			s.detach( parent );
			s.byId( "SimpleEntity" ).load( 1L );
			s.update( parent );

			// assert we still have an uninitialized proxy
			assertFalse( Hibernate.isInitialized( parent ) );

			// serialize/deserialize the proxy
			final Map<String, Object> deserializedParent =
					(Map<String, Object>) SerializationHelper.clone( (Serializable) parent );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.get( "name" ) );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testProxyInitializationWithoutTX() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final Map<String, Object> child = (Map<String, Object>) s.load( "ChildEntity", 1L );

			final Map<String, Object> parent = (Map<String, Object>) child.get( "parent" );

			t.rollback();
			session.close();

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof MapProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			assertEquals( "TheParent", parent.get( "name" ) );

			// assert we have an initialized proxy now
			assertTrue( Hibernate.isInitialized( parent ) );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-7686")
	public void testProxyInitializationWithoutTXAfterDeserialization() {
		final Session s = openSession();

		final Transaction t = s.beginTransaction();
		try {
			final Map<String, Object> child = (Map<String, Object>) s.load( "ChildEntity", 1L );

			final Map<String, Object> parent = (Map<String, Object>) child.get( "parent" );

			// destroy AbstractLazyInitializer internal state
			final Map<String, Object> deserializedParent =
					(Map<String, Object>) SerializationHelper.clone( (Serializable) parent );

			t.rollback();
			session.close();

			// assert we have an uninitialized proxy
			assertTrue( deserializedParent instanceof MapProxy );
			assertFalse( Hibernate.isInitialized( deserializedParent ) );

			assertEquals( "TheParent", deserializedParent.get( "name" ) );

			// assert we have an initialized proxy now
			assertTrue( Hibernate.isInitialized( deserializedParent ) );
		}
		finally {
			if ( t.isActive() ) {
				t.rollback();
			}
			s.close();
		}
	}

}