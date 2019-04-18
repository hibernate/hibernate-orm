/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */

@TestForIssue( jiraKey = "HHH-11147" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class DeepInheritanceProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testRootGetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertFalse( Hibernate.isInitialized( aEntity) );
					aEntity.getFieldInAMappedSuperclass();
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertFalse( Hibernate.isInitialized( aEntity) );
					aEntity.getFieldInAEntity();
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testRootSetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertFalse( Hibernate.isInitialized( aEntity) );
					aEntity.setFieldInAMappedSuperclass( (short) 3 );
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertFalse( Hibernate.isInitialized( aEntity) );
					aEntity.setFieldInAEntity( false );
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.get( AEntity.class, "AEntity" );

					assertTrue( Hibernate.isInitialized( aEntity) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testMiddleGetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertFalse( Hibernate.isInitialized( aaEntity) );
					aaEntity.getFieldInAMappedSuperclass();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertFalse( Hibernate.isInitialized( aaEntity) );
					aaEntity.getFieldInAEntity();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertFalse( Hibernate.isInitialized( aaEntity) );
					aaEntity.getFieldInAAEntity();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testMiddleSetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertFalse( Hibernate.isInitialized( aaEntity ) );
					aaEntity.setFieldInAMappedSuperclass( (short) 3 );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertFalse( Hibernate.isInitialized( aaEntity) );
					aaEntity.setFieldInAEntity( false );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertFalse( Hibernate.isInitialized( aaEntity) );
					aaEntity.setFieldInAAEntity( "updated field in AAEntity" );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "updated field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.get( AAEntity.class, "AAEntity" );

					assertTrue( Hibernate.isInitialized( aaEntity) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "updated field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testLeafGetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity) );
					aaaEntity.getFieldInAMappedSuperclass();
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity) );
					aaaEntity.getFieldInAEntity();
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity) );
					aaaEntity.getFieldInAAEntity();
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testLeafSetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					aaaEntity.setFieldInAMappedSuperclass( (short) 3 );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity) );
					aaaEntity.setFieldInAEntity( false );

					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity) );
					aaaEntity.setFieldInAAEntity( "updated field in AAEntity" );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "updated field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( Hibernate.isInitialized( aaaEntity) );
					aaaEntity.setFieldInAAAEntity( 4 );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "updated field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 4, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.get( AAAEntity.class, "AAAEntity" );

					assertTrue( Hibernate.isInitialized( aaaEntity) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 3, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "updated field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 4, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( AMappedSuperclass.class );
		sources.addAnnotatedClass( AEntity.class );
		sources.addAnnotatedClass( AAEntity.class );
		sources.addAnnotatedClass( AAAEntity.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					AEntity aEntity = new AEntity( "AEntity" );
					aEntity.setFieldInAMappedSuperclass( (short) 2 );
					aEntity.setFieldInAEntity( true );
					session.persist( aEntity );

					AAEntity aaEntity = new AAAEntity( "AAEntity" );
					aaEntity.setFieldInAMappedSuperclass( (short) 2 );
					aaEntity.setFieldInAEntity( true );
					aaEntity.setFieldInAAEntity( "field in AAEntity" );
					session.persist( aaEntity );

					AAAEntity aaaEntity = new AAAEntity( "AAAEntity" );
					aaaEntity.setFieldInAMappedSuperclass( (short) 2 );
					aaaEntity.setFieldInAEntity( true );
					aaaEntity.setFieldInAAEntity( "field in AAEntity" );
					aaaEntity.setFieldInAAAEntity( 3 );
					session.persist( aaaEntity );
				}
		);
	}

	@After
	public void clearTestData(){
		inTransaction(
				session -> {
					session.createQuery( "delete from AEntity" ).executeUpdate();
				}
		);
	}

	@MappedSuperclass
	public static class AMappedSuperclass implements Serializable {

		@Id
		private String id;

		private short fieldInAMappedSuperclass;

		public short getFieldInAMappedSuperclass() {
			return fieldInAMappedSuperclass;
		}

		public void setFieldInAMappedSuperclass(short fieldInAMappedSuperclass) {
			this.fieldInAMappedSuperclass = fieldInAMappedSuperclass;
		}

		public AMappedSuperclass(String id) {
			this.id = id;
		}

		protected AMappedSuperclass() {
		}
	}

	@Entity(name="AEntity")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class AEntity extends AMappedSuperclass {

		private Boolean fieldInAEntity;

		public AEntity(String id) {
			super(id);
		}

		protected AEntity() {
		}

		public Boolean getFieldInAEntity() {
			return fieldInAEntity;
		}
		public void setFieldInAEntity(Boolean fieldInAEntity) {
			this.fieldInAEntity = fieldInAEntity;
		}
	}

	@Entity(name="AAEntity")
	public static class AAEntity extends AEntity {

		private String fieldInAAEntity;

		public AAEntity(String id) {
			super(id);
		}

		protected AAEntity() {
		}

		public String getFieldInAAEntity() {
			return fieldInAAEntity;
		}

		public void setFieldInAAEntity(String fieldInAAEntity) {
			this.fieldInAAEntity = fieldInAAEntity;
		}
	}

	@Entity(name="AAAEntity")
	public static class AAAEntity extends AAEntity {

		private long fieldInAAAEntity;

		public AAAEntity(String id) {
			super(id);
		}

		protected AAAEntity() {
		}

		public long getFieldInAAAEntity() {
			return fieldInAAAEntity;
		}

		public void setFieldInAAAEntity(long fieldInAAAEntity) {
			this.fieldInAAAEntity = fieldInAAAEntity;
		}
	}
}
