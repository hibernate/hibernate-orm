/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.basic;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.jdbc.SQLStatementInspector;
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

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ EnhancerTestContext.class, NoDirtyCheckingContext.class })
@TestForIssue(jiraKey = { "HHH-15634", "HHH-16049" })
public class OnlyLazyBasicUpdateTest extends BaseCoreFunctionalTestCase {

	private Long entityId;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { LazyEntity.class };
	}

	@Override
	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		super.prepareBasicRegistryBuilder( serviceRegistryBuilder );
		serviceRegistryBuilder.applySetting( AvailableSettings.STATEMENT_INSPECTOR, SQLStatementInspector.class.getName() );
	}

	SQLStatementInspector statementInspector() {
		return (SQLStatementInspector) sessionFactory().getSessionFactoryOptions().getStatementInspector();
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
			entity.setLazyProperty1( "lazy1_initial" );
			entity.setLazyProperty2( "lazy2_initial" );
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@Before
	public void clearStatementInspector() {
		statementInspector().clear();
	}

	@Test
	public void updateSomeLazyProperty_nullToNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );

		// When a lazy property is modified Hibernate does not perform any select
		// but during flush an update is performed
		statementInspector().assertUpdate();
	}

	@Test
	public void updateSomeLazyProperty_nullToNonNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertNull( entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateSomeLazyProperty_nonNullToNonNull_differentValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( "lazy1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertEquals( "lazy1_update", entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateSomeLazyProperty_nonNullToNonNull_sameValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( entity.getLazyProperty1() );
		} );

		// We should not update entities when property values did not change
		statementInspector().assertNoUpdate();
	}

	@Test
	public void updateSomeLazyProperty_nonNullToNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			assertNull( entity.getLazyProperty1() );

			assertEquals( "lazy2_initial", entity.getLazyProperty2() );
		} );
	}

	@Test
	public void updateAllLazyProperties_nullToNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( null );
			entity.setLazyProperty2( null );
		} );

		// When a lazy property is modified Hibernate does not perform any select
		// but during flush an update is performed
		statementInspector().assertUpdate();
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
		} );
	}

	@Test
	public void updateAllLazyProperties_nonNullToNonNull_differentValues() {
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
		} );
	}

	@Test
	public void updateAllLazyProperties_nonNullToNonNull_sameValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			LazyEntity entity = s.get( LazyEntity.class, entityId );
			entity.setLazyProperty1( entity.getLazyProperty1() );
			entity.setLazyProperty2( entity.getLazyProperty2() );
		} );

		// We should not update entities when property values did not change
		statementInspector().assertNoUpdate();
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
