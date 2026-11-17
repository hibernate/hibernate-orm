/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.proxy.concrete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A to-one association of an audited entity whose target is the abstract root
 * of a {@link ConcreteProxy} hierarchy can be read at a revision.
 */
@EnversTest
@Jpa(annotatedClasses = {
		ConcreteProxyToOneTest.BaseEntity.class,
		ConcreteProxyToOneTest.SubEntity.class,
		ConcreteProxyToOneTest.ReferencingEntity.class
})
@JiraKey("HHH-21188")
public class ConcreteProxyToOneTest {

	@AfterEach
	public void dropData(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testFindAtRevision(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final SubEntity sub = new SubEntity( 1L, "sub" );
			entityManager.persist( sub );
			entityManager.persist( new ReferencingEntity( 1L, sub ) );
		} );

		scope.inEntityManager( entityManager -> {
			final ReferencingEntity referencing = AuditReaderFactory.get( entityManager )
					.find( ReferencingEntity.class, 1L, 1 );

			final Object target = Hibernate.unproxy( referencing.getTarget() );
			assertThat( target ).isInstanceOf( SubEntity.class );
			assertThat( ( (SubEntity) target ).getName() ).isEqualTo( "sub" );
		} );
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	@ConcreteProxy
	@Audited
	public static abstract class BaseEntity {
		@Id
		private Long id;

		private String name;

		public BaseEntity() {
		}

		public BaseEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "SubEntity")
	@Audited
	public static class SubEntity extends BaseEntity {
		public SubEntity() {
		}

		public SubEntity(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "ReferencingEntity")
	@Audited
	public static class ReferencingEntity {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private BaseEntity target;

		public ReferencingEntity() {
		}

		public ReferencingEntity(Long id, BaseEntity target) {
			this.id = id;
			this.target = target;
		}

		public Long getId() {
			return id;
		}

		public BaseEntity getTarget() {
			return target;
		}
	}
}
