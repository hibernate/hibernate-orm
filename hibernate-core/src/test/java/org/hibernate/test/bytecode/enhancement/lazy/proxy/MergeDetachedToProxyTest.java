/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;


import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-11147" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class MergeDetachedToProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testMergeInitializedDetachedOntoProxy() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();

		final AEntity aEntityDetached = fromTransaction(
				session -> {
					AEntity aEntity = session.get( AEntity.class, 1 );
					assertIsEnhancedProxy( aEntity.bEntity );
					Hibernate.initialize( aEntity.bEntity );
					return aEntity;
				}
		);

		statistics.clear();
		assertThat( statistics.getPrepareStatementCount(), is( 0L ) );

		inSession(
				session -> {
					BEntity bEntity = session.getReference( BEntity.class, 2 );
					assertIsEnhancedProxy( bEntity );
					assertThat( statistics.getPrepareStatementCount(), is( 0L ) );

					AEntity aEntityMerged = (AEntity) session.merge( aEntityDetached );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertSame( bEntity, aEntityMerged.bEntity );
					assertEquals( "a description", aEntityDetached.bEntity.description );
					assertTrue( Hibernate.isInitialized( bEntity ) );
				}
		);

		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testMergeUpdatedDetachedOntoProxy() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();

		final AEntity aEntityDetached = fromTransaction(
				session -> {
					AEntity aEntity = session.get( AEntity.class, 1 );
					assertIsEnhancedProxy( aEntity.bEntity );
					Hibernate.initialize( aEntity.bEntity );
					return aEntity;
				}
		);

		aEntityDetached.bEntity.description = "new description";

		statistics.clear();
		assertThat( statistics.getPrepareStatementCount(), is( 0L ) );

		inSession(
				session -> {
					BEntity bEntity = session.getReference( BEntity.class, 2 );
					assertIsEnhancedProxy( bEntity );
					assertThat( statistics.getPrepareStatementCount(), is( 0L ) );

					AEntity aEntityMerged = (AEntity) session.merge( aEntityDetached );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertSame( bEntity, aEntityMerged.bEntity );
					assertEquals( "new description", aEntityDetached.bEntity.description );
					assertTrue( Hibernate.isInitialized( bEntity ) );
				}
		);

		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
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
		sources.addAnnotatedClass( AEntity.class );
		sources.addAnnotatedClass( BEntity.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					final AEntity aEntity = new AEntity();
					aEntity.id = 1;
					final BEntity bEntity = new BEntity();
					bEntity.id = 2;
					bEntity.description = "a description";
					aEntity.bEntity = bEntity;
					session.persist( aEntity );
				}
		);
	}

	@After
	public void clearTestData(){
		inTransaction(
				session -> {
					session.createQuery( "delete from AEntity" ).executeUpdate();
					session.createQuery( "delete from BEntity" ).executeUpdate();
				}
		);
	}

	private void assertIsEnhancedProxy(Object entity) {
		assertTrue( PersistentAttributeInterceptable.class.isInstance( entity ) );

		final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) entity;
		final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
		assertTrue( EnhancementAsProxyLazinessInterceptor.class.isInstance( interceptor ) );
	}

	@Entity(name = "AEntity")
	public static class AEntity {
		@Id
		private int id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private BEntity bEntity;
	}

	@Entity(name = "BEntity")
	public static class BEntity {
		@Id
		private int id;

		private String description;

	}
}
