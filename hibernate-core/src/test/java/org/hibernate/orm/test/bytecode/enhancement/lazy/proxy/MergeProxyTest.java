/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.proxy.HibernateProxy;

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

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 * @author Gail BadnerMergeProxyTest
 */
@JiraKey( "HHH-11147" )
@DomainModel(
		annotatedClasses = {
				Activity.class, Instruction.class, WebApplication.class
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
public class MergeProxyTest {

	@Test
	@JiraKey("HHH-11147")
	public void testMergeDetachUninitializedProxy(SessionFactoryScope scope) {
		final Activity activity = scope.fromTransaction(
				session -> session.get( Activity.class, 0 )
		);

		assertThat( Hibernate.isInitialized( activity ), is( true ) );
		assertThat( Hibernate.isPropertyInitialized( activity, "instruction" ), is( true ) );
		final Instruction instruction = activity.getInstruction();
		assertThat( Hibernate.isInitialized( instruction ), is( false ) );
		assertThat( instruction, instanceOf( PersistentAttributeInterceptable.class ) );
		final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) instruction ).$$_hibernate_getInterceptor();
		assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

		activity.setNbr( "2" );

		final Activity mergedActivity = scope.fromTransaction(
				session -> (Activity) session.merge( activity )
		);

		assertTrue( Hibernate.isInitialized( mergedActivity ) );
		assertThat( activity, not( CoreMatchers.sameInstance( mergedActivity ) ) );

		scope.inTransaction(
				session -> {
					final Instruction persistentInstruction = session.get( Instruction.class, 0 );
					assertThat( persistentInstruction.getSummary(), is( "Instruction #0" ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-11147")
	public void testMergeDetachInitializedProxy(SessionFactoryScope scope) {
		final Activity activityProxy = scope.fromTransaction(
				session -> {
					final Activity activity = session.getReference( Activity.class, 0 );
					assertFalse( Hibernate.isInitialized( activity) );
					Hibernate.initialize( activity );
					return activity;
				}
		);

		assertThat( activityProxy, not( instanceOf( HibernateProxy.class ) ) );
		assertThat( Hibernate.isInitialized( activityProxy), is( true ) );

		{
			assertThat( Hibernate.isPropertyInitialized( activityProxy, "instruction" ), is( true ) );
			final Instruction instruction = activityProxy.getInstruction();
			assertThat( instruction, instanceOf( PersistentAttributeInterceptable.class ) );
			final PersistentAttributeInterceptor instructionInterceptor = ( (PersistentAttributeInterceptable) instruction ).$$_hibernate_getInterceptor();
			assertThat( instructionInterceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );
		}

		{
			assertThat( Hibernate.isPropertyInitialized( activityProxy, "webApplication" ), is( true ) );
			assertThat( activityProxy.getWebApplication(), nullValue() );
		}

		activityProxy.setNbr( "2" );

		final Activity mergedActivity = scope.fromTransaction(
				session -> (Activity) session.merge( activityProxy )
		);

		assertTrue( Hibernate.isInitialized( mergedActivity ) );
		assertThat( activityProxy, not( CoreMatchers.sameInstance( mergedActivity ) ) );

		scope.inTransaction(
				session -> {
					final Instruction instruction = session.get( Instruction.class, 0 );
					assertThat( instruction.getSummary(), is( "Instruction #0" ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-11147")
	public void testMergeDetachInitializedByAccessProxy(SessionFactoryScope scope) {
		final Activity activityProxy = scope.fromTransaction(
				session -> {
					final Activity activity = session.getReference( Activity.class, 0 );
					assertFalse( Hibernate.isInitialized( activity) );
					activity.getDescription();
					return activity;
				}
		);

		assertThat( activityProxy, not( instanceOf( HibernateProxy.class ) ) );
		assertThat( Hibernate.isInitialized( activityProxy), is( true ) );

		{
			assertThat( Hibernate.isPropertyInitialized( activityProxy, "instruction" ), is( true ) );
			final Instruction instruction = activityProxy.getInstruction();
			assertThat( instruction, instanceOf( PersistentAttributeInterceptable.class ) );
			final PersistentAttributeInterceptor instructionInterceptor = ( (PersistentAttributeInterceptable) instruction ).$$_hibernate_getInterceptor();
			assertThat( instructionInterceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );
		}

		{
			assertThat( Hibernate.isPropertyInitialized( activityProxy, "webApplication" ), is( true ) );
			assertThat( activityProxy.getWebApplication(), nullValue() );
		}

		activityProxy.setNbr( "2" );

		final Activity mergedActivity = scope.fromTransaction(
				session -> (Activity) session.merge( activityProxy )
		);

		assertTrue( Hibernate.isInitialized( mergedActivity ) );
		assertThat( activityProxy, not( CoreMatchers.sameInstance( mergedActivity ) ) );

		scope.inTransaction(
				session -> {
					final Instruction instruction = session.get( Instruction.class, 0 );
					assertThat( instruction.getSummary(), is( "Instruction #0" ) );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// create a slew of Activity objects, some with Instruction reference
					// some without.

					for ( int i = 0; i < 30; i++ ) {
						final Activity activity = new Activity( i, "Activity #" + i, null );
						if ( i % 2 == 0 ) {
							final Instruction instr = new Instruction( i, "Instruction #" + i );
							activity.setInstruction( instr );
							session.persist( instr );
						}
						else {
							final WebApplication webApplication = new WebApplication( i, "http://" + i + ".com" );
							webApplication.setName( "name #" + i );
							activity.setWebApplication( webApplication );
							webApplication.getActivities().add( activity );
							session.persist( webApplication );
						}

						session.persist( activity );
					}
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
