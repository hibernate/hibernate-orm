/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

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
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
@DomainModel(annotatedClasses = {
		EnhancedEntityProxySerializationTest.SimpleEntity.class, EnhancedEntityProxySerializationTest.ChildEntity.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-4451")
public class EnhancedEntityProxySerializationTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity entity = new SimpleEntity();
			entity.setId( 1L );
			entity.setName( "TheParent" );

			final ChildEntity c1 = new ChildEntity();
			c1.setId( 1L );
			c1.setParent( entity );

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
			assertFalse( Hibernate.isInitialized( deserializedChild.parent ) );
			assertEquals( "TheParent", deserializedChild.parent.getName() );
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
