/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.basic;

import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
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

@DomainModel(
		annotatedClasses = {
				EagerAndLazyBasicUpdateTest.LazyEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ EnhancerTestContext.class, NoDirtyCheckingContext.class })
@JiraKey("HHH-15634")
@JiraKey("HHH-16049")
public class EagerAndLazyBasicUpdateTest {

	private Long entityId;

	SQLStatementInspector statementInspector(SessionFactoryScope scope) {
		return (SQLStatementInspector) scope.getSessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	private void initNull(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			LazyEntity entity = new LazyEntity();
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	private void initNonNull(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			LazyEntity entity = new LazyEntity();
			entity.setEagerProperty( "eager_initial" );
			entity.setLazyProperty1( "lazy1_initial" );
			entity.setLazyProperty2( "lazy2_initial" );
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@BeforeEach
	public void clearStatementInspector(SessionFactoryScope scope) {
		statementInspector( scope ).clear();
	}

	@Test
	public void updateOneLazyProperty_nullToNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );

		// When a lazy property is modified Hibernate does not perform any select
		// but during flush an update is performed
		statementInspector( scope ).assertUpdate();
	}

	@Test
	public void updateOneLazyProperty_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertNull( entity.getEagerProperty() );
			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneLazyProperty_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneLazyProperty_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );

		// When a lazy property is modified Hibernate does not perform any select
		// but during flush an update is performed
		statementInspector( scope ).assertUpdate();
	}

	@Test
	public void updateOneLazyProperty_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerProperty_nullToNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( null );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateOneEagerProperty_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update", entity.getEagerProperty() );

			assertNull( entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerProperty_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update", entity.getEagerProperty() );

			assertEquals( "lazy1_initial", entity.getLazyProperty1() );
			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerProperty_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_initial" );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateOneEagerProperty_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getEagerProperty() );

			assertEquals( "lazy1_initial", entity.getLazyProperty1() );
			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nullToNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( null );
			entity.setLazyProperty1( null );
		} );

		// When a lazy property is modified Hibernate does not perform any select
		// but during flush an update is performed
		statementInspector( scope ).assertUpdate();
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update" );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update", entity.getEagerProperty() );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update" );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update", entity.getEagerProperty() );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( entity.getEagerProperty() );
			entity.setLazyProperty1( entity.getLazyProperty1() );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( null );
			entity.setLazyProperty1( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getEagerProperty() );
			assertNull( entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nullToNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
			entity.setLazyProperty2( null );
		} );

		// When a lazy property is modified Hibernate does not perform any select
		// but during flush an update is performed
		statementInspector( scope ).assertUpdate();
	}

	@Test
	public void updateAllLazyProperties_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
			entity.setLazyProperty2( "lazy2_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );
			assertEquals( "lazy2_update", entity.getLazyProperty2() );

			assertNull( entity.getEagerProperty() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
			entity.setLazyProperty2( "lazy2_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );
			assertEquals( "lazy2_update", entity.getLazyProperty2() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( entity.getLazyProperty1() );
			entity.setLazyProperty2( entity.getLazyProperty2() );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateAllLazyProperties_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
			entity.setLazyProperty2( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
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
		String lazyProperty1;
		// We need multiple lazy properties to reproduce the problem.
		@Basic(fetch = FetchType.LAZY)
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
