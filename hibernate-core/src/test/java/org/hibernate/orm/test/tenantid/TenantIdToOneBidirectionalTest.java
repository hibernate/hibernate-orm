/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import jakarta.persistence.Column;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		TenantIdToOneBidirectionalTest.RootEntity.class,
		TenantIdToOneBidirectionalTest.ChildEntity.class,
} )
@SessionFactory
@ServiceRegistry( settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18617" )
public class TenantIdToOneBidirectionalTest implements SessionFactoryProducer {
	private static String currentTenant;

	@Test
	public void testExistingRoot(SessionFactoryScope scope) {
		currentTenant = "tenant_1";
		scope.inTransaction( session -> {
			final var child = session.find( ChildEntity.class, 1L );
			assertThat( child.getRoot() ).isNotNull().extracting( RootEntity::getChild ).isSameAs( child );
		} );
	}

	@Test
	public void testRemovedRoot(SessionFactoryScope scope) {
		currentTenant = "tenant_2";
		scope.inTransaction( session -> {
			final var child = session.find( ChildEntity.class, 2L );
			assertThat( child.getRoot() ).isNull();
		} );
	}

	@Test
	public void testNoRoot(SessionFactoryScope scope) {
		currentTenant = "tenant_3";
		scope.inTransaction( session -> {
			final var child = session.find( ChildEntity.class, 3L );
			assertThat( child.getRoot() ).isNull();
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		currentTenant = "tenant_1";
		scope.inTransaction( session -> {
			final var withChild = new RootEntity( 1L );
			withChild.setChild( new ChildEntity( 1L ) );
			session.persist( withChild );
		} );
		currentTenant = "tenant_2";
		scope.inTransaction( session -> {
			final var deletedRoot = new RootEntity( 2L );
			final var child = new ChildEntity( 2L );
			deletedRoot.setChild( child );
			session.persist( deletedRoot );

			session.flush();
			session.clear();
			session.remove( deletedRoot );
		} );
		currentTenant = "tenant_3";
		scope.inTransaction( session -> session.persist( new ChildEntity( 3L ) ) );
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sfb = model.getSessionFactoryBuilder();
		sfb.applyCurrentTenantIdentifierResolver( new CurrentTenantIdentifierResolver<String>() {
			@Override
			public String resolveCurrentTenantIdentifier() {
				return currentTenant;
			}

			@Override
			public boolean validateExistingCurrentSessions() {
				return false;
			}
		} );
		return (SessionFactoryImplementor) sfb.build();
	}

	@Entity( name = "RootEntity" )
	static class RootEntity {
		@Id
		private Long id;

		@TenantId
		@Column(name="tenant_col")
		private String tenant;

		@OneToOne( cascade = CascadeType.PERSIST )
		@JoinColumn
		private ChildEntity child;

		public RootEntity() {
		}

		public RootEntity(Long id) {
			this.id = id;
		}

		public ChildEntity getChild() {
			return child;
		}

		public void setChild(ChildEntity child) {
			this.child = child;
		}
	}

	@Entity( name = "ChildEntity" )
	static class ChildEntity {
		@Id
		private Long id;

		@TenantId
		@Column(name="tenant_col")
		private String tenant;

		@OneToOne( mappedBy = "child" )
		private RootEntity root;

		public ChildEntity() {
		}

		public ChildEntity(Long id) {
			this.id = id;
		}

		public RootEntity getRoot() {
			return root;
		}
	}
}
