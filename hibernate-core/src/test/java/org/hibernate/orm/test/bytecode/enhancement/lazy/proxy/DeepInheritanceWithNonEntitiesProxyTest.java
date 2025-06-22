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
import org.hibernate.proxy.HibernateProxy;
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
				DeepInheritanceWithNonEntitiesProxyTest.AMappedSuperclass.class,
				DeepInheritanceWithNonEntitiesProxyTest.AEntity.class,
				DeepInheritanceWithNonEntitiesProxyTest.AAEntity.class,
				DeepInheritanceWithNonEntitiesProxyTest.AAAEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class DeepInheritanceWithNonEntitiesProxyTest {

	@Test
	public void testRootGetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property
					aEntity.getFieldInAMappedSuperclass();
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property
					aEntity.getFieldInAEntity();
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( (short) 2, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testRootGetValueInNonEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property (even in a non-entity)
					aEntity.getFieldInNonEntityAMappedSuperclassSuperclass();
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity) );
					// Gets initialized when access any property (even in a non-entity)
					aEntity.getFieldInNonEntityAEntitySuperclass();
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property
					aEntity.setFieldInAMappedSuperclass( (short) 3 );
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property
					aEntity.setFieldInAEntity( false );
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
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

					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( (short) 3, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( false, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testRootSetValueInNonEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property (even in a non-entity)
					aEntity.setFieldInNonEntityAMappedSuperclassSuperclass( (short) 5 );
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 5, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					assertEquals( 6, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aEntity.getFieldInNonEntityAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.getReference( AEntity.class, "AEntity" );

					assertTrue( HibernateProxy.class.isInstance( aEntity ) );
					assertFalse( Hibernate.isInitialized( aEntity ) );
					// Gets initialized when access any property (even in a non-entity)
					aEntity.setFieldInNonEntityAEntitySuperclass( 10L );
					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 10 ), aEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					assertEquals( 6, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aEntity.getFieldInNonEntityAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AEntity aEntity = session.get( AEntity.class, "AEntity" );

					assertTrue( Hibernate.isInitialized( aEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aEntity.getFieldInAEntity() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity ) );
					// Gets initialized when access any property
					aaEntity.getFieldInAMappedSuperclass();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity ) );
					// Gets initialized when access any property
					aaEntity.getFieldInAEntity();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity) );
					// Gets initialized when access any property
					aaEntity.getFieldInAAEntity();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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
	public void testMiddleGetValueInNonEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity) );
					// Gets initialized when access any property (even in a non-entity)
					aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity) );
					// Gets initialized when access any property (even in a non-entity)
					aaEntity.getFieldInNonEntityAEntitySuperclass();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity) );
					// Gets initialized when access any property (even in a non-entity)
					aaEntity.getFieldInNonEntityAAEntitySuperclass();
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity ) );
					// Gets initialized when access any property
					aaEntity.setFieldInAMappedSuperclass( (short) 3 );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity ) );
					// Gets initialized when access any property
					aaEntity.setFieldInAEntity( false );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity ) );
					// Gets initialized when access any property
					aaEntity.setFieldInAAEntity( "updated field in AAEntity" );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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

					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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
	public void testMiddleSetValueInNonEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity ) );
					// Gets initialized when access any property (even in a non-entity)
					aaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( (short) 10 );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 10, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					assertEquals( 6, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaEntity.getFieldInNonEntityAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity) );
					// Gets initialized when access any property (even in a non-entity)
					aaEntity.setFieldInNonEntityAEntitySuperclass( 4L );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 4 ), aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					assertEquals( 6, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaEntity.getFieldInNonEntityAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.getReference( AAEntity.class, "AAEntity" );

					assertTrue( HibernateProxy.class.isInstance( aaEntity ) );
					assertFalse( Hibernate.isInitialized( aaEntity) );
					// Gets initialized when access any property (even in a non-entity)
					aaEntity.setFieldInNonEntityAAEntitySuperclass( "xyz" );
					assertTrue( Hibernate.isInitialized( aaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "xyz", aaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( (short) 2, aaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					assertEquals( 6, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaEntity.getFieldInNonEntityAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAEntity aaEntity = session.get( AAEntity.class, "AAEntity" );

					assertTrue( Hibernate.isInitialized( aaEntity) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaEntity.getFieldInNonEntityAAEntitySuperclass() );
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
	public void testLeafGetValueToInitialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity gets initialized when a persistent property is accessed
					aaaEntity.getFieldInAMappedSuperclass();
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity gets initialized when a persistent property is accessed
					aaaEntity.getFieldInAEntity();
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity gets initialized when a persistent property is accessed
					aaaEntity.getFieldInAAEntity();
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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
	public void testLeafGetValueInNonEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass();
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.getFieldInNonEntityAEntitySuperclass();
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity only gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.getFieldInNonEntityAAEntitySuperclass();
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.getFieldInNonEntityAAAEntitySuperclass();
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity only gets initialized when a persistent property is accessed
					aaaEntity.setFieldInAMappedSuperclass( (short) 3 );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity only gets initialized when a persistent property is accessed
					aaaEntity.setFieldInAEntity( false );

					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity only gets initialized when a persistent property is accessed
					aaaEntity.setFieldInAAEntity( "updated field in AAEntity" );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity only gets initialized when a persistent property is accessed
					aaaEntity.setFieldInAAAEntity( 4 );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

	@Test
	public void testLeafSetValueInNonEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 99 );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 99, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					aaaEntity.setFieldInNonEntityAAAEntitySuperclass( false );
					assertEquals( 6, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( false, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.setFieldInNonEntityAEntitySuperclass( 10L );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 10 ), aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					aaaEntity.setFieldInNonEntityAAAEntitySuperclass( false );
					assertEquals( 6, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( false, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.setFieldInNonEntityAAEntitySuperclass( "xyz" );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "xyz", aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );

					assertEquals( 0, stats.getPrepareStatementCount() );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					// aaaEntity only gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					aaaEntity.setFieldInNonEntityAAAEntitySuperclass( false );
					assertEquals( 6, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( false, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.getReference( AAAEntity.class, "AAAEntity" );

					assertFalse( HibernateProxy.class.isInstance( aaaEntity ) );
					assertFalse( Hibernate.isInitialized( aaaEntity) );
					// aaaEntity is not a HibernateProxy
					// aaaEntity will not get intialized when a non-entity property is accessed
					aaaEntity.setFieldInNonEntityAAAEntitySuperclass( true );
					assertFalse( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( true, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );

					assertFalse( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					// aaaEntity gets initialized when a persistent property is accessed
					assertEquals( (short) 2, aaaEntity.getFieldInAMappedSuperclass() );
					assertTrue( Hibernate.isInitialized( aaaEntity ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( true, aaaEntity.getFieldInAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( "field in AAEntity", aaaEntity.getFieldInAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 3, aaaEntity.getFieldInAAAEntity() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// set the properties that are in non-entity classes after initialization
					aaaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 6 );
					aaaEntity.setFieldInNonEntityAEntitySuperclass( 104L );
					aaaEntity.setFieldInNonEntityAAEntitySuperclass( "?" );
					aaaEntity.setFieldInNonEntityAAAEntitySuperclass( false );
					assertEquals( 6, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( Long.valueOf( 104 ), aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( "?", aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( false, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
				}
		);

		scope.inTransaction(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					AAAEntity aaaEntity = session.get( AAAEntity.class, "AAAEntity" );

					assertTrue( Hibernate.isInitialized( aaaEntity ) );

					assertEquals( 1, stats.getPrepareStatementCount() );
					assertEquals( 0, aaaEntity.getFieldInNonEntityAMappedSuperclassSuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAEntitySuperclass() );
					assertEquals( null, aaaEntity.getFieldInNonEntityAAAEntitySuperclass() );
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

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AEntity aEntity = new AEntity( "AEntity" );
					aEntity.setFieldInAMappedSuperclass( (short) 2 );
					aEntity.setFieldInAEntity( true );
					aEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 3 );
					aEntity.setFieldInNonEntityAEntitySuperclass( 4L );
					session.persist( aEntity );

					AAEntity aaEntity = new AAAEntity( "AAEntity" );
					aaEntity.setFieldInAMappedSuperclass( (short) 2 );
					aaEntity.setFieldInAEntity( true );
					aaEntity.setFieldInAAEntity( "field in AAEntity" );
					aaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 3 );
					aaEntity.setFieldInNonEntityAEntitySuperclass( 4L );
					aaEntity.setFieldInNonEntityAAEntitySuperclass( "abc" );
					session.persist( aaEntity );

					AAAEntity aaaEntity = new AAAEntity( "AAAEntity" );
					aaaEntity.setFieldInAMappedSuperclass( (short) 2 );
					aaaEntity.setFieldInAEntity( true );
					aaaEntity.setFieldInAAEntity( "field in AAEntity" );
					aaaEntity.setFieldInAAAEntity( 3 );
					aaaEntity.setFieldInNonEntityAMappedSuperclassSuperclass( 3 );
					aaaEntity.setFieldInNonEntityAEntitySuperclass( 4L );
					aaaEntity.setFieldInNonEntityAAEntitySuperclass( "abc" );
					aaaEntity.setFieldInNonEntityAAAEntitySuperclass( true );
					session.persist( aaaEntity );
				}
		);
	}

	@AfterEach
	public void clearTestData(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public static class NonEntityAMappedSuperclassSuperclass {
		private int fieldInNonEntityAMappedSuperclassSuperclass;

		public int getFieldInNonEntityAMappedSuperclassSuperclass() {
			return fieldInNonEntityAMappedSuperclassSuperclass;
		}

		public void setFieldInNonEntityAMappedSuperclassSuperclass(int fieldInNonEntityAMappedSuperclassSuperclass) {
			this.fieldInNonEntityAMappedSuperclassSuperclass = fieldInNonEntityAMappedSuperclassSuperclass;
		}
	}

	@MappedSuperclass
	public static class AMappedSuperclass extends NonEntityAMappedSuperclassSuperclass implements Serializable {

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

	public static class NonEntityAEntitySuperclass extends AMappedSuperclass {

		private Long fieldInNonEntityAEntitySuperclass;

		public NonEntityAEntitySuperclass(String id) {
			super( id );
		}

		protected NonEntityAEntitySuperclass() {
		}

		public Long getFieldInNonEntityAEntitySuperclass() {
			return fieldInNonEntityAEntitySuperclass;
		}

		public void setFieldInNonEntityAEntitySuperclass(Long fieldInNonEntityAEntitySuperclass) {
			this.fieldInNonEntityAEntitySuperclass = fieldInNonEntityAEntitySuperclass;
		}
	}

	@Entity(name="AEntity")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class AEntity extends NonEntityAEntitySuperclass {

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

	public static class NonEntityAAEntitySuperclass extends AEntity {

		private String fieldInNonEntityAAEntitySuperclass;

		public NonEntityAAEntitySuperclass(String id) {
			super( id );
		}

		protected NonEntityAAEntitySuperclass() {
		}

		public String getFieldInNonEntityAAEntitySuperclass() {
			return fieldInNonEntityAAEntitySuperclass;
		}

		public void setFieldInNonEntityAAEntitySuperclass(String fieldInNonEntityAAEntitySuperclass) {
			this.fieldInNonEntityAAEntitySuperclass = fieldInNonEntityAAEntitySuperclass;
		}
	}

	@Entity(name="AAEntity")
	public static class AAEntity extends NonEntityAAEntitySuperclass {

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

	public static class NonEntityAAAEntitySuperclass extends AAEntity {

		private Boolean fieldInNonEntityAAAEntitySuperclass;

		public NonEntityAAAEntitySuperclass(String id) {
			super( id );
		}

		protected NonEntityAAAEntitySuperclass() {
		}

		public Boolean getFieldInNonEntityAAAEntitySuperclass() {
			return fieldInNonEntityAAAEntitySuperclass;
		}

		public void setFieldInNonEntityAAAEntitySuperclass(Boolean fieldInNonEntityAAAEntitySuperclass) {
			this.fieldInNonEntityAAAEntitySuperclass = fieldInNonEntityAAAEntitySuperclass;
		}
	}

	@Entity(name="AAAEntity")
	public static class AAAEntity extends NonEntityAAAEntitySuperclass {

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
