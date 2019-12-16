/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class MappedSuperclassWithEmbeddableTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			TestEntity testEntity = new TestEntity( "2", "test" );
			s.persist( testEntity );
		} );
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "delete from TestEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testIt() {
		doInHibernate( this::sessionFactory, s -> {
			TestEntity testEntity = s.get( TestEntity.class, "2" );
			assertThat( testEntity, notNullValue() );
		} );
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		@Embedded
		private EmbeddedValue superField;

		public EmbeddedValue getSuperField() {
			return superField;
		}

		public void setSuperField(EmbeddedValue superField) {
			this.superField = superField;
		}
	}

	@Embeddable
	public static class EmbeddedValue implements Serializable {
		@Column(name = "super_field")
		private String superField;

		public EmbeddedValue() {
		}

		private EmbeddedValue(String superField) {
			this.superField = superField;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddedValue that = (EmbeddedValue) o;
			return superField.equals( that.superField );
		}

		@Override
		public int hashCode() {
			return Objects.hash( superField );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends BaseEntity {
		@Id
		private String id;
		private String name;

		public TestEntity() {
		}

		private TestEntity(String id, String name) {
			this.id = id;
			this.name = name;
			EmbeddedValue value = new EmbeddedValue( "SUPER " + name );
			setSuperField( value );
		}


		public String id() {
			return id;
		}

		public String name() {
			return name;
		}
	}

}
