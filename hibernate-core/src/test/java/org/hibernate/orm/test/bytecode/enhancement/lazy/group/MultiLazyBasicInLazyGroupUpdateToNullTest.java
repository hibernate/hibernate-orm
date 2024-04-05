/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = {
				MultiLazyBasicInLazyGroupUpdateToNullTest.LazyEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext( { EnhancerTestContext.class, NoDirtyCheckingContext.class} )
public class MultiLazyBasicInLazyGroupUpdateToNullTest {

	private Long entityId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			LazyEntity entity = new LazyEntity();
			entity.setEagerProperty( "eager" );
			entity.setLazyProperty1( "update1" );
			entity.setLazyProperty2( "update2" );
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@Test
	public void updateOneLazyProperty(SessionFactoryScope scope) {
		// non-null -> null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );
			assertNotNull( entity.getLazyProperty2() );
			assertNotNull( entity.getEagerProperty() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty(SessionFactoryScope scope) {
		// non-null -> null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( null );
			entity.setLazyProperty1( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getEagerProperty() );
			assertNull( entity.getLazyProperty1() );
			assertNotNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateAllLazyProperties(SessionFactoryScope scope) {
		// non-null -> null
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
			entity.setLazyProperty2( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );
			assertNotNull( entity.getEagerProperty() );
		} );
	}

	@Entity
	@Table(name = "LAZY_ENTITY")
	static class LazyEntity {
		@Id
		@GeneratedValue
		Long id;
		// We need at least one eager property to avoid a different problem.
		@Basic
		String eagerProperty;
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group1")
		String lazyProperty1;
		// We need multiple lazy properties to reproduce the problem.
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group2")
		String lazyProperty2;

		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		public String getEagerProperty() {
			return eagerProperty;
		}

		public void setEagerProperty(String eagerProperty) {
			this.eagerProperty = eagerProperty;
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
