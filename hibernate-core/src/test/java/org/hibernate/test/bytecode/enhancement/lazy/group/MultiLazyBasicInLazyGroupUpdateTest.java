/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext( { EnhancerTestContext.class, NoDirtyCheckingContext.class} )
public class MultiLazyBasicInLazyGroupUpdateTest extends BaseCoreFunctionalTestCase {

	private Long entityId;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { LazyEntity.class };
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = new LazyEntity();
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@Test
	public void updateOneLazyProperty() {
		// null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update1" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update1", entity.getLazyProperty1() );
			assertNull(entity.getLazyProperty2());
			assertNull(entity.getEagerProperty());
		} );

		// non-null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update2" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update2", entity.getLazyProperty1() );
			assertNull(entity.getLazyProperty2());
			assertNull(entity.getEagerProperty());
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty() {
		// null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update1" );
			entity.setLazyProperty1( "update1" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update1", entity.getEagerProperty() );
			assertEquals( "update1", entity.getLazyProperty1() );
			assertNull(entity.getLazyProperty2());
		} );

		// non-null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update2" );
			entity.setLazyProperty1( "update2" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update2", entity.getEagerProperty() );
			assertEquals( "update2", entity.getLazyProperty1() );
			assertNull(entity.getLazyProperty2());
		} );
	}

	@Test
	public void updateAllLazyProperties() {
		// null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update1" );
			entity.setLazyProperty2( "update2_1" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update1", entity.getLazyProperty1() );
			assertEquals( "update2_1", entity.getLazyProperty2() );
			assertNull( entity.getEagerProperty() );
		} );

		// non-null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update2" );
			entity.setLazyProperty2( "update2_2" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update2", entity.getLazyProperty1() );
			assertEquals( "update2_2", entity.getLazyProperty2() );
			assertNull( entity.getEagerProperty() );
		} );
	}

	@Entity
	@Table(name = "LAZY_ENTITY")
	private static class LazyEntity {
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
