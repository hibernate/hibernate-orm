/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.basic;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Tests the behavior of basic property updates when all properties are eager (the default).
 * <p>
 * This is mostly for comparison with {@link EagerAndLazyBasicUpdateTest}/{@link OnlyLazyBasicUpdateTest},
 * because the mere presence of lazy properties in one entity may affect the behavior of eager properties, too.
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ EnhancerTestContext.class, NoDirtyCheckingContext.class })
@TestForIssue(jiraKey = "HHH-16049")
public class OnlyEagerBasicUpdateTest extends BaseCoreFunctionalTestCase {

	private Long entityId;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EagerEntity.class };
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
			EagerEntity entity = new EagerEntity();
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	private void initNonNull() {
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = new EagerEntity();
			entity.setEagerProperty1( "eager1_initial" );
			entity.setEagerProperty2( "eager2_initial" );
			s.persist( entity );
			entityId = entity.getId();
		} );
	}

	@Before
	public void clearStatementInspector() {
		statementInspector().clear();
	}

	@Test
	public void updateSomeEagerProperty_nullToNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
		} );

		// We should not update entities when property values did not change
		statementInspector().assertNoUpdate();
	}

	@Test
	public void updateSomeEagerProperty_nullToNonNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );

			assertNull( entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateSomeEagerProperty_nonNullToNonNull_differentValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );

			assertEquals( "eager2_initial", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateSomeEagerProperty_nonNullToNonNull_sameValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( entity.getEagerProperty1() );
		} );

		// We should not update entities when property values did not change
		statementInspector().assertNoUpdate();
	}

	@Test
	public void updateSomeEagerProperty_nonNullToNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertNull( entity.getEagerProperty1() );

			assertEquals( "eager2_initial", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateAllEagerProperties_nullToNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
			entity.setEagerProperty2( null );
		} );

		// We should not update entities when property values did not change
		statementInspector().assertNoUpdate();
	}

	@Test
	public void updateAllEagerProperties_nullToNonNull() {
		initNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
			entity.setEagerProperty2( "eager2_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );
			assertEquals( "eager2_update", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateAllEagerProperties_nonNullToNonNull_differentValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( "eager1_update" );
			entity.setEagerProperty2( "eager2_update" );
		} );
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertEquals( "eager1_update", entity.getEagerProperty1() );
			assertEquals( "eager2_update", entity.getEagerProperty2() );
		} );
	}

	@Test
	public void updateAllEagerProperties_nonNullToNonNull_sameValues() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( entity.getEagerProperty1() );
			entity.setEagerProperty2( entity.getEagerProperty2() );
		} );

		// We should not update entities when property values did not change
		statementInspector().assertNoUpdate();
	}

	@Test
	public void updateAllEagerProperties_nonNullToNull() {
		initNonNull();
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			entity.setEagerProperty1( null );
			entity.setEagerProperty2( null );
		} );
		doInHibernate( this::sessionFactory, s -> {
			EagerEntity entity = s.get( EagerEntity.class, entityId );
			assertNull( entity.getEagerProperty1() );
			assertNull( entity.getEagerProperty2() );
		} );
	}

	@Entity
	@Table(name = "LAZY_ENTITY")
	private static class EagerEntity {
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
