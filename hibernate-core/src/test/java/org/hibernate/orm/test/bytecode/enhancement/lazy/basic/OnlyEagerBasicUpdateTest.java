/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;

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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tests the behavior of basic property updates when all properties are eager (the default).
 * <p>
 * This is mostly for comparison with {@link EagerAndLazyBasicUpdateTest}/{@link OnlyLazyBasicUpdateTest},
 * because the mere presence of lazy properties in one entity may affect the behavior of eager properties, too.
 */
@DomainModel(
		annotatedClasses = {
				OnlyEagerBasicUpdateTest.EagerEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ EnhancerTestContext.class, NoDirtyCheckingContext.class })
@JiraKey("HHH-16049")
public class OnlyEagerBasicUpdateTest {

	private Long entityId;

	SQLStatementInspector statementInspector(SessionFactoryScope scope) {
		return (SQLStatementInspector) scope.getSessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	private void initNull(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EagerEntity entity = new EagerEntity();
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	private void initNonNull(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EagerEntity entity = new EagerEntity();
			entity.setEagerProperty1( "eager1_initial" );
			entity.setEagerProperty2( "eager2_initial" );
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@BeforeEach
	public void clearStatementInspector(SessionFactoryScope scope) {
		statementInspector( scope ).clear();
	}

	@Test
	public void updateSomeEagerProperty_nullToNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateSomeEagerProperty_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
		} );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );

			assertNull( entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateSomeEagerProperty_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
		} );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );

			assertEquals( "eager2_initial", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateSomeEagerProperty_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( entity.getEagerProperty1() );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateSomeEagerProperty_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
		} );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertNull( entity.getEagerProperty1() );

			assertEquals( "eager2_initial", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateAllEagerProperties_nullToNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
			entity.setEagerProperty2( null );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateAllEagerProperties_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
			entity.setEagerProperty2( "eager2_update" );
		} );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );
			assertEquals( "eager2_update", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateAllEagerProperties_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
			entity.setEagerProperty2( "eager2_update" );
		} );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );
			assertEquals( "eager2_update", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateAllEagerProperties_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( entity.getEagerProperty1() );
			entity.setEagerProperty2( entity.getEagerProperty2() );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateAllEagerProperties_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
			entity.setEagerProperty2( null );
		} );
		scope.inTransaction( s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertNull( entity.getEagerProperty1() );
			assertNull( entity.getEagerProperty2() );
		} );
	}

	@Entity
	@Table(name = "LAZY_ENTITY")
	static class EagerEntity {
		@Id
		@GeneratedValue
		Long id;
		@Basic
		String eagerProperty1;
		@Basic
		String eagerProperty2;

		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		public String getEagerProperty1() {
			return eagerProperty1;
		}

		public void setEagerProperty1(String eagerProperty1) {
			this.eagerProperty1 = eagerProperty1;
		}

		public String getEagerProperty2() {
			return eagerProperty2;
		}

		public void setEagerProperty2(String eagerProperty2) {
			this.eagerProperty2 = eagerProperty2;
		}
	}
}
