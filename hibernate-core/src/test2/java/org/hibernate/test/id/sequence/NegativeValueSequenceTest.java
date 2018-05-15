/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.sequence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class NegativeValueSequenceTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, SequenceStyleGenerator.class.getName() )
	);

	@Test
	@TestForIssue( jiraKey = "HHH-5933")
	public void testNegativeOneAllocationSizeNoopOptimizer() {
		ServiceRegistryImplementor serviceRegistry = null;
		SessionFactoryImplementor sessionFactory = null;
		Session session = null;
		try {
			serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.build();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000116" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( NegativeOneIncrementSize.class )
					.buildMetadata();

			// NegativeOneIncrementSize ID has allocationSize == -1, so warning should not be triggered.
			assertEquals( false, triggerable.wasTriggered() );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

			assertOptimizer( sessionFactory, NegativeOneIncrementSize.class, NoopOptimizer.class );

			session = sessionFactory.openSession();
			session.getTransaction().begin();

			// initial value is -10; sequence should be decremented by 1 (since allocationSize is -1)
			for ( Integer i = -10; i >= -15; i-- ) {
				NegativeOneIncrementSize theEntity = new NegativeOneIncrementSize();
				session.persist( theEntity );
				assertEquals( i, theEntity.id );
			}
		}
		finally {
			if ( session != null ) {
				session.getTransaction().rollback();
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5933")
	public void testNegativeTwoAllocationSizeNoopOptimizer() {
		ServiceRegistryImplementor serviceRegistry = null;
		SessionFactoryImplementor sessionFactory = null;
		Session session = null;
		try {
			serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.build();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000116" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( NegativeTwoIncrementSize.class )
					.buildMetadata();

			// NegativeTwoIncrementSize ID has allocationSize == -2, so warning should be triggered.
			assertEquals( true, triggerable.wasTriggered() );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

			assertOptimizer( sessionFactory, NegativeTwoIncrementSize.class, NoopOptimizer.class );

			session = sessionFactory.openSession();
			session.getTransaction().begin();

			// initial value is -10; sequence should be decremented by 1
			// (since negative NoopOptimizer negative default is -1)
			for ( Integer i = -10; i >= -15; i-- ) {
				NegativeTwoIncrementSize theEntity = new NegativeTwoIncrementSize();
				session.persist( theEntity );
				assertEquals( i, theEntity.id );
			}
		}
		finally {
			if ( session != null ) {
				session.getTransaction().rollback();
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11709" )
	public void testPositiveOneAllocationSizeNoopOptimizer() {
		ServiceRegistryImplementor serviceRegistry = null;
		SessionFactoryImplementor sessionFactory = null;
		Session session = null;
		try {
			serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.build();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000116" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( PositiveOneIncrementSize.class )
					.buildMetadata();

			// PositiveOneIncrementSize ID has allocationSize == 1, so warning should not be triggered.
			assertEquals( false, triggerable.wasTriggered() );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

			assertOptimizer( sessionFactory, PositiveOneIncrementSize.class, NoopOptimizer.class );

			session = sessionFactory.openSession();
			session.getTransaction().begin();

			// initial value is -5; sequence should be incremented by 1 (since allocationSize is 1)
			for ( Integer i = -5; i <= 5; i++ ) {
				PositiveOneIncrementSize theEntity = new PositiveOneIncrementSize();
				session.persist( theEntity );
				assertEquals( i, theEntity.id );
			}
		}
		finally {
			if ( session != null ) {
				session.getTransaction().rollback();
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11709" )
	public void testPositiveTwoAllocationSizeNoopOptimizer() {
		ServiceRegistryImplementor serviceRegistry = null;
		SessionFactoryImplementor sessionFactory = null;
		Session session = null;
		try {
			serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "none" )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.build();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000116" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( PositiveTwoIncrementSize.class )
					.buildMetadata();

			// NoopOptimizer is preferred (due to setting AvailableSettings.PREFERRED_POOLED_OPTIMIZER to "false")
			// PositiveTwoIncrementSize ID has allocationSize == 2, so warning should be triggered.
			assertEquals( true, triggerable.wasTriggered() );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

			assertOptimizer( sessionFactory, PositiveTwoIncrementSize.class, NoopOptimizer.class );

			session = sessionFactory.openSession();
			session.getTransaction().begin();

			// initial value is -5; sequence should be incremented by 1
			// (since NoopOptimizer positive default allocationSize is 1)
			for ( Integer i = -5; i <= 5; i++ ) {
				PositiveTwoIncrementSize theEntity = new PositiveTwoIncrementSize();
				session.persist( theEntity );
				assertEquals( i, theEntity.id );
			}
		}
		finally {
			if ( session != null ) {
				session.getTransaction().rollback();
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11709" )
	public void testPositiveTwoAllocationSizePooledOptimizer() {
		ServiceRegistryImplementor serviceRegistry = null;
		SessionFactoryImplementor sessionFactory = null;
		Session session = null;
		try {
			serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.build();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000116" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( PositiveTwoIncrementSize.class )
					.buildMetadata();

			// PositiveTwoIncrementSize ID has allocationSize == 2, so PooledOptimizer should be used.
			// Warning should not be triggered.
			assertEquals( false, triggerable.wasTriggered() );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

			assertOptimizer( sessionFactory, PositiveTwoIncrementSize.class, PooledOptimizer.class );

			session = sessionFactory.openSession();
			session.getTransaction().begin();

			// initial value is -5; sequence should be incremented by 1
			// (since NoopOptimizer positive default allocationSize is 1)
			for ( Integer i = -5; i <= 5; i++ ) {
				PositiveTwoIncrementSize theEntity = new PositiveTwoIncrementSize();
				session.persist( theEntity );
				assertEquals( i, theEntity.id );
			}
		}
		finally {
			if ( session != null ) {
				session.getTransaction().rollback();
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11712" )
	public void testNegativeTwoAllocationSizePositiveStartNoopOptimizer() {
		ServiceRegistryImplementor serviceRegistry = null;
		SessionFactoryImplementor sessionFactory = null;
		Session session = null;
		try {
			serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.build();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000116" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( NegativeTwoIncrementSizePositiveInitialValue.class )
					.buildMetadata();

			// NegativeTwoIncrementSizePositiveInitialValue ID has allocationSize == -2, so warning should be triggered.
			assertEquals( true, triggerable.wasTriggered() );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();

			assertOptimizer( sessionFactory, NegativeTwoIncrementSizePositiveInitialValue.class, NoopOptimizer.class );

			session = sessionFactory.openSession();
			session.getTransaction().begin();

			// initial value is 5; sequence should be decremented by 1
			// (since negative NoopOptimizer negative default is -1)
			for ( Integer i = 5; i <= -5; i-- ) {
				NegativeTwoIncrementSizePositiveInitialValue theEntity =
						new NegativeTwoIncrementSizePositiveInitialValue();
				session.persist( theEntity );
				assertEquals( i, theEntity.id );
			}
		}
		finally {
			if ( session != null ) {
				session.getTransaction().rollback();
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			if ( serviceRegistry != null ) {
				serviceRegistry.destroy();
			}
		}
	}

	private void assertOptimizer(
			SessionFactoryImplementor sessionFactory,
			Class<?> entityClass,
			Class<? extends Optimizer> expectedOptimizerClass) {
		assertTrue(
				SequenceStyleGenerator.class.isInstance(
						sessionFactory.getMetamodel()
								.entityPersister( entityClass )
								.getIdentifierGenerator()
				)
		);
		SequenceStyleGenerator generator = (SequenceStyleGenerator) sessionFactory.getMetamodel()
				.entityPersister( entityClass )
				.getIdentifierGenerator();
		assertTrue( expectedOptimizerClass.isInstance( generator.getOptimizer() ) );
	}

	@Entity( name = "NegativeOneIncrementSize" )
	@Table( name = "NegativeOneIncrementSize" )
	public static class NegativeOneIncrementSize {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
		@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "NEG_ONE_SEQ", initialValue= -10, allocationSize = -1)
		public Integer id;
	}

	@Entity( name = "NegativeTwoIncrementSize" )
	@Table( name = "NegativeTwoIncrementSize" )
	public static class NegativeTwoIncrementSize {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
		@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "NEG_TWO_SEQ", initialValue= -10, allocationSize = -2)
		public Integer id;
	}

	@Entity( name = "PositiveOneIncrementSize" )
	@Table( name = "PositiveOneIncrementSize" )
	public static class PositiveOneIncrementSize {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
		@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "POS_ONE_SEQ", initialValue= -5, allocationSize = 1)
		public Integer id;
	}

	@Entity( name = "PositiveTwoIncrementSize" )
	@Table( name = "PositiveTwoIncrementSize" )
	public static class PositiveTwoIncrementSize {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
		@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "POS_TWO_SEQ", initialValue= -5, allocationSize = 2)
		public Integer id;
	}

	@Entity( name = "NegativeTwoIncrSizePosStart" )
	@Table( name = "NegativeTwoIncrSizePosStart" )
	public static class NegativeTwoIncrementSizePositiveInitialValue {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
		@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "NEG_TWO_INCR_POS_START_SEQ", initialValue= 5, allocationSize = -2)
		public Integer id;
	}
}
