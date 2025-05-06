/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.map.MapProxy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Selaron
 */
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/serialization/DynamicMapMappings.hbm.xml"
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
})
public class MapProxySerializationTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
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

			s.persist( "SimpleEntity", entity );
			s.persist( "ChildEntity", c1 );
		} );
	}

	/**
	 * Tests that serializing an initialized proxy will serialize the target instead.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@JiraKey(value = "HHH-7686")
	public void testInitializedProxySerializationIfTargetInPersistenceContext(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Map<String, Object> child = (Map<String, Object>) s.getReference( "ChildEntity", 1L );

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
		} );
	}

	/**
	 * Tests that serializing a proxy which is not initialized
	 * but whose target has been (separately) added to the persistence context
	 * will serialized the target instead.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@JiraKey(value = "HHH-7686")
	public void testUninitializedProxySerializationIfTargetInPersistenceContext(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Map<String, Object> child = (Map<String, Object>) s.getReference( "ChildEntity", 1L );

			final Map<String, Object> parent = (Map<String, Object>) child.get( "parent" );

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof MapProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Load the target of the proxy without the proxy being made aware of it
			s.detach( parent );
			s.byId( "SimpleEntity" ).load( 1L );
			Map<String, Object> merged = s.merge( parent );

			assertTrue( Hibernate.isInitialized( merged ) );
			assertFalse( Hibernate.isInitialized( parent ) );

			// serialize/deserialize the proxy
			final Map<String, Object> deserializedParent =
					(Map<String, Object>) SerializationHelper.clone( (Serializable) merged );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.get( "name" ) );
		} );
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testProxyInitializationWithoutTX(SessionFactoryScope scope) {
		final Map<String, Object> parent = scope.fromTransaction( s -> {
			final Map<String, Object> child = (Map<String, Object>) s.getReference( "ChildEntity", 1L );
			return (Map<String, Object>) child.get( "parent" );
		});
		// assert we have an uninitialized proxy
		assertTrue( parent instanceof MapProxy );
		assertFalse( Hibernate.isInitialized( parent ) );

		assertEquals( "TheParent", parent.get( "name" ) );

		// assert we have an initialized proxy now
		assertTrue( Hibernate.isInitialized( parent ) );
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@JiraKey(value = "HHH-7686")
	public void testProxyInitializationWithoutTXAfterDeserialization(SessionFactoryScope scope) {
		final Map<String, Object> deserializedParent = scope.fromTransaction( s -> {
			final Map<String, Object> child = (Map<String, Object>) s.getReference( "ChildEntity", 1L );
			final Map<String, Object> parent = (Map<String, Object>) child.get( "parent" );

			// destroy AbstractLazyInitializer internal state
			return (Map<String, Object>) SerializationHelper.clone( (Serializable) parent );
		});

		// assert we have an uninitialized proxy
		assertTrue( deserializedParent instanceof MapProxy );
		assertFalse( Hibernate.isInitialized( deserializedParent ) );

		assertEquals( "TheParent", deserializedParent.get( "name" ) );

		// assert we have an initialized proxy now
		assertTrue( Hibernate.isInitialized( deserializedParent ) );
	}

}
