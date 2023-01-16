/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.basic;

import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext( {EnhancerTestContext.class, NoDirtyCheckingContext.class} )
@TestForIssue(jiraKey = "HHH-15634")
public class EagerAndLazyBasicUpdateTest extends BaseCoreFunctionalTestCase {

	private Long entityId;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { LazyEntity.class };
	}

	private void initNull() {
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = new LazyEntity();
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	private void initNonNull() {
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = new LazyEntity();
			entity.setEagerProperty( "eager_initial" );
			entity.setLazyProperty1( "lazy1_initial" );
			entity.setLazyProperty2( "lazy2_initial" );
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@Test
	public void updateOneLazyProperty_nullToNonNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertNull( entity.getEagerProperty() );
			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneLazyProperty_nonNullToNonNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneLazyProperty_nonNullToNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nullToNonNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update" );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update", entity.getEagerProperty() );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nonNullToNonNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( "eager_update" );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "eager_update", entity.getEagerProperty() );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateOneEagerPropertyAndOneLazyProperty_nonNullToNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setEagerProperty( null );
			entity.setLazyProperty1( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getEagerProperty() );
			assertNull( entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nullToNonNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
			entity.setLazyProperty2( "lazy2_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );
			assertEquals( "lazy2_update", entity.getLazyProperty2() );

			assertNull( entity.getEagerProperty() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nonNullToNonNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
			entity.setLazyProperty2( "lazy2_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );
			assertEquals( "lazy2_update", entity.getLazyProperty2() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nonNullToNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
			entity.setLazyProperty2( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );
			assertNull( entity.getLazyProperty2() );

			assertEquals( "eager_initial", entity.getEagerProperty() );
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
