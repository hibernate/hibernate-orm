/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		LazyOneToOneWithCastTest.EntityA.class,
		LazyOneToOneWithCastTest.EntityB.class,
})
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class LazyOneToOneWithCastTest {

	@Test
	void oneNullOneNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA();
			entityA.setId( 1 );

			final EntityB entityB = new EntityB();
			entityB.setId( 2 );
			entityB.setToOneWithCast( entityA );
			entityA.setToOneWithCast( entityB );
			entityB.setLazyString( "lazy" );

			session.persist( entityB );
			session.persist( entityA );

		} );

		scope.inTransaction( session -> {
			final EntityB entityB = session.find( EntityB.class, 2 );

			EntityA toOne = entityB.getToOne();
			assertThat( toOne ).isNull();
			assertThat( Hibernate.isPropertyInitialized( entityB, "toOne" ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( entityB, "toOneWithCast" ) ).isFalse();
			assertThat( Hibernate.isPropertyInitialized( entityB, "lazyString" ) ).isFalse();

			// update lazy-basic property in same lazy-group
			entityB.setLazyString( "lazy_updated" );
			assertThat( Hibernate.isPropertyInitialized( entityB, "lazyString" ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( entityB, "toOneWithCast" ) ).isFalse();

			// now access the lazy to-one with cast
			final EntityA toOneWithCast = entityB.getToOneWithCast();
			assertThat( Hibernate.isPropertyInitialized( entityB, "toOneWithCast" ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( entityB, "lazyString" ) ).isTrue();
			assertThat( toOneWithCast ).isNotNull().extracting( EntityA::getId ).isEqualTo( 1 );
			assertThat( entityB.getLazyString() ).isEqualTo( "lazy_updated" );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "EntityA")
	static class EntityA {
		@Id
		private Integer id;

		@OneToOne
		@JoinColumn(name = "entity_b_id")
		private EntityB toOne;

		@OneToOne(targetEntity = EntityB.class)
		@JoinColumn(name = "entity_b_id_cast")
		private EntityB toOneWithCast;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getToOne() {
			return toOne;
		}

		public void setToOne(EntityB containedIndexedEmbedded) {
			this.toOne = containedIndexedEmbedded;
		}

		public Object getToOneWithCast() {
			return toOneWithCast;
		}

		public void setToOneWithCast(EntityB containedIndexedEmbeddedWithCast) {
			this.toOneWithCast = containedIndexedEmbeddedWithCast;
		}
	}

	@Entity(name = "EntityB")
	static class EntityB {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "toOne", fetch = FetchType.LAZY)
		@LazyGroup("toOneGroup")
		private EntityA toOne;

		@OneToOne(mappedBy = "toOneWithCast", targetEntity = EntityA.class, fetch = FetchType.LAZY)
		@LazyGroup("toOneWithCastGroup")
		private EntityA toOneWithCast;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("toOneWithCastGroup")
		private String lazyString;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getToOne() {
			return toOne;
		}

		public void setToOne(EntityA toOne) {
			this.toOne = toOne;
		}

		public EntityA getToOneWithCast() {
			return toOneWithCast;
		}

		public void setToOneWithCast(EntityA toOneWithCast) {
			this.toOneWithCast = toOneWithCast;
		}

		public String getLazyString() {
			return lazyString;
		}

		public void setLazyString(String lazyString) {
			this.lazyString = lazyString;
		}
	}
}
