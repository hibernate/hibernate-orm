/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.transaction.TransactionUtil.fromTransaction;
import static org.hibernate.testing.orm.transaction.TransactionUtil.inTransaction;

/**
 * @author Marco Belladelli
 */
@BaseUnitTest
public class ProxySerializationNoSessionFactoryTest {
	@Test
	public void testUninitializedProxy() {
		executeTest( false );
	}

	@Test
	public void testInitializedProxy() {
		executeTest( true );
	}

	private void executeTest(boolean initializeProxy) {
		SessionFactoryRegistry.INSTANCE.clearRegistrations();
		final Configuration cfg = new Configuration()
				.setProperty( AvailableSettings.HBM2DDL_AUTO, Action.ACTION_CREATE_THEN_DROP )
				.addAnnotatedClass( SimpleEntity.class )
				.addAnnotatedClass( ChildEntity.class );
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		final SimpleEntity parent;
		try (final SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg.buildSessionFactory()) {
			inTransaction( factory, session -> {
						final SimpleEntity entity = new SimpleEntity();
						entity.setId( 1L );
						entity.setName( "TheParent" );
						session.persist( entity );

						final ChildEntity child = new ChildEntity();
						child.setId( 1L );
						child.setParent( entity );
						session.persist( child );
					}
			);
			parent = fromTransaction( factory, session -> {
						final ChildEntity childEntity = session.find( ChildEntity.class, 1L );
						final SimpleEntity entity = childEntity.getParent();
						if ( initializeProxy ) {
							assertThat( entity.getName() ).isEqualTo( "TheParent" );
						}
						return entity;
					}
			);
		}

		// The session factory is not available anymore
		assertThat( SessionFactoryRegistry.INSTANCE.hasRegistrations() ).isFalse();

		assertThat( parent instanceof HibernateProxy ).isTrue();
		assertThat( Hibernate.isInitialized( parent ) ).isEqualTo( initializeProxy );

		// Serialization and deserialization should still work
		final SimpleEntity clone = (SimpleEntity) SerializationHelper.clone( parent );
		assertThat( clone ).isNotNull();
		assertThat( clone.getId() ).isEqualTo( parent.getId() );
		if ( initializeProxy ) {
			assertThat( clone.getName() ).isEqualTo( parent.getName() );
		}
	}

	@Entity(name = "SimpleEntity")
	static class SimpleEntity implements Serializable {
		@Id
		private Long id;

		private String name;

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
	}

	@Entity(name = "ChildEntity")
	static class ChildEntity {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		private SimpleEntity parent;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public SimpleEntity getParent() {
			return parent;
		}

		public void setParent(SimpleEntity parent) {
			this.parent = parent;
		}
	}
}
