/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
@DomainModel(annotatedClasses = {
		EnhancedEntityProxySerializationTest.SimpleEntity.class, EnhancedEntityProxySerializationTest.ChildEntity.class
})
@SessionFactory
@JiraKey("HHH-4451")
public class EnhancedEntityProxySerializationTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// idempotent across tests: data is committed once and only read afterwards
			final Number count = (Number) session.createQuery( "SELECT count(id) FROM SimpleEntity" ).getSingleResult();
			if ( count.longValue() > 0L ) {
				return;
			}
			final SimpleEntity entity = new SimpleEntity();
			entity.setId( 1L );
			entity.setName( "TheParent" );

			final ChildEntity c1 = new ChildEntity();
			c1.setId( 1L );
			c1.setParent( entity );
			c1.setDescription( "ChildOne" );

			final ChildEntity c2 = new ChildEntity();
			c2.setId( 2L );
			c2.setParent( entity );

			session.persist( entity );
			session.persist( c1 );
			session.persist( c2 );
		} );
	}

	@Test
	public void testDeserialization(SessionFactoryScope scope) {
		byte[] serialized = scope.fromSession( session -> {
			// Load entity and proxy into session
			ChildEntity child = session.find( ChildEntity.class, 1L );
			assertFalse( Hibernate.isInitialized( child.parent ) );

			//fake the in container work
			session.getJdbcCoordinator().getLogicalConnection().manualDisconnect();
			return SerializationHelper.serialize( session );
		} );

		final SessionImplementor deserializedSession = (SessionImplementor) SerializationHelper.deserialize( serialized );
		scope.inTransaction( deserializedSession, session -> {
			final ChildEntity deserializedChild = session.find( ChildEntity.class, 1L );

			// ENHANCED_PROXY branch: stub parent's interceptor reinjected
			assertFalse( Hibernate.isInitialized( deserializedChild.parent ) );
			assertEquals( "TheParent", deserializedChild.parent.getName() );

			// INITIALIZED branch: child's lazy basic attribute interceptor reinjected
			assertFalse( Hibernate.isPropertyInitialized( deserializedChild, "description" ) );
			assertEquals( "ChildOne", deserializedChild.getDescription() );
		} );
	}

	@Test
	public void testInitializedLazyFieldsPreserved(SessionFactoryScope scope) {
		final byte[] serialized = scope.fromSession( session -> {
			final ChildEntity child = session.find( ChildEntity.class, 1L );
			// Force-initialize the lazy @Basic attribute: the interceptor records "description"
			// in its initializedLazyFields set.
			child.getDescription();
			assertTrue( Hibernate.isPropertyInitialized( child, "description" ) );

			//fake the in container work
			session.getJdbcCoordinator().getLogicalConnection().manualDisconnect();
			return SerializationHelper.serialize( session );
		} );

		final SessionImplementor deserializedSession = (SessionImplementor) SerializationHelper.deserialize( serialized );
		scope.inTransaction( deserializedSession, session -> {
			final ChildEntity deserializedChild = session.find( ChildEntity.class, 1L );

			assertTrue( Hibernate.isPropertyInitialized( deserializedChild, "description" ) );
			assertEquals( "ChildOne", deserializedChild.getDescription() );
		} );
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
	public static class ChildEntity implements Serializable {
		private Long id;

		private SimpleEntity parent;
		private String description;

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

		@Basic(fetch = FetchType.LAZY)
		public String getDescription(){
			return description;
		}
		public void setDescription(final String description){
			this.description = description;
		}
	}
}
