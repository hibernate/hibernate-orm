/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Selaron
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		EntityProxySerializationTest.SimpleEntity.class, EntityProxySerializationTest.ChildEntity.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" )
})
public class EntityProxySerializationTest {

	/**
	 * Prepare and persist a {@link SimpleEntity} with two {@link ChildEntity}.
	 */
	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Number count = (Number) s.createQuery("SELECT count(ID) FROM SimpleEntity").getSingleResult();
			if (count.longValue() > 0L) {
				// entity already added previously
				return;
			}

			final SimpleEntity entity = new SimpleEntity();
			entity.setId( 1L );
			entity.setName( "TheParent" );

			final ChildEntity c1 = new ChildEntity();
			c1.setId( 1L );
			c1.setParent( entity );

			final ChildEntity c2 = new ChildEntity();
			c2.setId( 2L );
			c2.setParent( entity );

			s.persist( entity );
			s.persist( c1 );
			s.persist( c2 );
		} );
	}

	/**
	 * Tests that serializing an initialized proxy will serialize the target instead.
	 */
	@Test
	public void testInitializedProxySerializationIfTargetInPersistenceContext(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final ChildEntity child = s.find( ChildEntity.class, 1L );

			final SimpleEntity parent = child.getParent();

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof HibernateProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Initialize the proxy
			parent.getName();
			assertTrue( Hibernate.isInitialized( parent ) );

			// serialize/deserialize the proxy
			final SimpleEntity deserializedParent = (SimpleEntity) SerializationHelper.clone( parent );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.getName() );
		} );
	}

	/**
	 * Tests that serializing a proxy which is not initialized
	 * but whose target has been (separately) added to the persistence context
	 * will serialize the target instead.
	 */
	@Test
	public void testUninitializedProxySerializationIfTargetInPersistenceContext(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final ChildEntity child = s.find( ChildEntity.class, 1L );

			final SimpleEntity parent = child.getParent();

			// assert we have an uninitialized proxy
			assertTrue( parent instanceof HibernateProxy );
			assertFalse( Hibernate.isInitialized( parent ) );

			// Load the target of the proxy without the proxy being made aware of it
			s.detach( parent );
			s.find( SimpleEntity.class, 1L );
			SimpleEntity merged = s.merge( parent );

			// assert we still have an uninitialized proxy
			assertFalse( Hibernate.isInitialized( parent ) );
			assertTrue( Hibernate.isInitialized( merged ) );

			// serialize/deserialize the proxy
			final SimpleEntity deserializedParent = (SimpleEntity) SerializationHelper.clone( merged );

			// assert the deserialized object is no longer a proxy, but the target of the proxy
			assertFalse( deserializedParent instanceof HibernateProxy );
			assertEquals( "TheParent", deserializedParent.getName() );
		} );
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@Test
	public void testProxyInitializationWithoutTX(SessionFactoryScope scope) {
		final SimpleEntity parent = scope.fromTransaction( s -> {
			final ChildEntity child = s.find( ChildEntity.class, 1L );
			return child.getParent();
		});

		// assert we have an uninitialized proxy
		assertTrue( parent instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( parent ) );

		assertEquals( "TheParent", parent.getName() );

		// assert we have an initialized proxy now
		assertTrue( Hibernate.isInitialized( parent ) );
	}

	/**
	 * Tests that lazy loading without transaction nor open session is generally
	 * working. The magic is done by {@link AbstractLazyInitializer} who opens a
	 * temporary session.
	 */
	@Test
	@JiraKey(value = "HHH-12720")
	public void testProxyInitializationWithoutTXAfterDeserialization(SessionFactoryScope scope) {
		final SimpleEntity deserializedParent = scope.fromTransaction( s -> {
			final ChildEntity child = s.find( ChildEntity.class, 1L );
			final SimpleEntity parent = child.getParent();
			// destroy AbstractLazyInitializer internal state
			return (SimpleEntity) SerializationHelper.clone( parent );
		});
		// assert we have an uninitialized proxy
		assertTrue( deserializedParent instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( deserializedParent ) );

		assertEquals( "TheParent", deserializedParent.getName() );

		// assert we have an initialized proxy now
		assertTrue( Hibernate.isInitialized( deserializedParent ) );
	}

	@Entity(name = "SimpleEntity")
	static class SimpleEntity implements Serializable {

		private Long id;

		private String name;

		Set<ChildEntity> children = new HashSet<>();

		@Id
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@OneToMany(targetEntity = ChildEntity.class, mappedBy = "parent")
		@Fetch(FetchMode.SELECT)
		public Set<ChildEntity> getChildren() {
			return children;
		}

		public void setChildren(final Set<ChildEntity> children) {
			this.children = children;
		}

	}

	@Entity(name = "ChildEntity")
	static class ChildEntity {
		private Long id;

		private SimpleEntity parent;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		public SimpleEntity getParent() {
			return parent;
		}

		public void setParent(final SimpleEntity parent) {
			this.parent = parent;
		}

	}
}
