/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.basic;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@RunWith(BytecodeEnhancerRunner.class)
public class OnlyLazyBasicUpdateTest extends BaseCoreFunctionalTestCase {

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
	public void updateSomeLazyProperty() {
		// null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update1" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update1", entity.getLazyProperty1() );
		} );

		// non-null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update2" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update2", entity.getLazyProperty1() );
		} );

		// non-null -> null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );
		} );
	}

	@Test
	public void updateAllLazyProperties() {
		// null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update1" );
			entity.setLazyProperty2( "update1" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update1", entity.getLazyProperty1() );
			assertEquals( "update1", entity.getLazyProperty2() );
		} );

		// non-null -> non-null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "update2" );
			entity.setLazyProperty2( "update2" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "update2", entity.getLazyProperty1() );
			assertEquals( "update2", entity.getLazyProperty2() );
		} );

		// non-null -> null
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
			entity.setLazyProperty2( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Entity
	@Table(name = "LAZY_ENTITY")
	private static class LazyEntity {
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
