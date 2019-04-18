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
import org.hibernate.proxy.HibernateProxy;
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
public class DeepInheritanceWithNonEntitiesProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testRootGetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testRootGetValueInNonEntity() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testRootSetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testRootSetValueInNonEntity() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testMiddleGetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testMiddleGetValueInNonEntity() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testMiddleSetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testMiddleSetValueInNonEntity() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testLeafGetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testLeafGetValueInNonEntity() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testLeafSetValueToInitialize() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testLeafSetValueInNonEntity() {
		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

		inTransaction(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

	@After
	public void clearTestData(){
		inTransaction(
				session -> {
					session.createQuery( "delete from AEntity" ).executeUpdate();
				}
		);
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
