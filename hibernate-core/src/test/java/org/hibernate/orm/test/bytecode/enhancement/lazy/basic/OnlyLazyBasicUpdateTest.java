/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.basic;

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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = {
				OnlyLazyBasicUpdateTest.LazyEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ EnhancerTestContext.class, NoDirtyCheckingContext.class })
@JiraKey("HHH-15634")
@JiraKey("HHH-16049")
public class OnlyLazyBasicUpdateTest {

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
	public void updateSomeLazyProperty_nullToNull(SessionFactoryScope scope) {
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
	public void updateSomeLazyProperty_nullToNonNull(SessionFactoryScope scope) {
		initNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateSomeLazyProperty_nonNullToNonNull_differentValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateSomeLazyProperty_nonNullToNonNull_sameValues(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( entity.getLazyProperty1() );
		} );

		// We should not update entities when property values did not change
		statementInspector( scope ).assertNoUpdate();
	}

	@Test
	public void updateSomeLazyProperty_nonNullToNull(SessionFactoryScope scope) {
		initNonNull( scope );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );
		scope.inTransaction( s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
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
		String lazyProperty1;
		@Basic(fetch = FetchType.LAZY)
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
