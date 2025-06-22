/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@DomainModel(
		annotatedClasses = {
				OnlyLazyBasicInLazyGroupBasicUpdateTest.LazyEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ EnhancerTestContext.class, NoDirtyCheckingContext.class })
public class OnlyLazyBasicInLazyGroupBasicUpdateTest {

	private Long entityId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			LazyEntity entity = new LazyEntity();
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@Test
	public void updateOneLazyProperty(SessionFactoryScope scope) {
		// null -> non-null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update1" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update1", entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );
		} );

		// non-null -> non-null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update2" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update2", entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateAllLazyProperties(SessionFactoryScope scope) {
		// null -> non-null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update1" );
			entity.setLazyProperty2( "update2_1" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update1", entity.getLazyProperty1() );
			assertEquals( "update2_1", entity.getLazyProperty2() );
		} );

		// non-null -> non-null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update2" );
			entity.setLazyProperty2( "update2_2" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update2", entity.getLazyProperty1() );
			assertEquals( "update2_2", entity.getLazyProperty2() );
		} );
	}

	@Entity
	@Table(name = "LAZY_ENTITY")
	static class LazyEntity {
		@Id
		@GeneratedValue
		Long id;
		// ALL properties must be lazy in order to reproduce the problem.
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group1")
		String lazyProperty1;
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group2")
		String lazyProperty2;

		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		public String getLazyProperty1() {
			return lazyProperty1;
		}

		public void setLazyProperty1(String lazyProperty1) {
			this.lazyProperty1 = lazyProperty1;
		}

		public String getLazyProperty2() {
			return lazyProperty2;
		}

		public void setLazyProperty2(String lazyProperty2) {
			this.lazyProperty2 = lazyProperty2;
		}
	}
}
