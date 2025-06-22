/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */

@JiraKey( "HHH-11147" )
@DomainModel(
		annotatedClasses = {
				DeepInheritanceProxyTest.AMappedSuperclass.class,
				DeepInheritanceProxyTest.AEntity.class,
				DeepInheritanceProxyTest.AAEntity.class,
				DeepInheritanceProxyTest.AAAEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class DeepInheritanceProxyTest {

	@Test
	public void testRootGetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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
	public void testRootSetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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
	public void testMiddleGetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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
	public void testMiddleSetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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
	public void testLeafGetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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
	public void testLeafSetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
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

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
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

	@AfterEach
	public void clearTestData(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
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
