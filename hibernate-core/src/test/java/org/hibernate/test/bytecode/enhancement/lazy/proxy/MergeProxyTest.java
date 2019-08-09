/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
@SuppressWarnings({"unused", "WeakerAccess","ResultOfMethodCallIgnored"})
@TestForIssue( jiraKey = "HHH-11147" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class MergeProxyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testMergeDetachUninitializedProxy() {
		final Activity activity = fromTransaction(
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

		final Activity mergedActivity = fromTransaction(
				session -> (Activity) session.merge( activity )
		);

		assertTrue( Hibernate.isInitialized( mergedActivity ) );
		assertThat( activity, not( CoreMatchers.sameInstance( mergedActivity ) ) );

		inTransaction(
				session -> {
					final Instruction persistentInstruction = session.get( Instruction.class, 0 );
					assertThat( persistentInstruction.getSummary(), is( "Instruction #0" ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testMergeDetachInitializedProxy() {
		final Activity activityProxy = fromTransaction(
				session -> {
					final Activity activity = session.load( Activity.class, 0 );
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

		final Activity mergedActivity = fromTransaction(
				session -> (Activity) session.merge( activityProxy )
		);

		assertTrue( Hibernate.isInitialized( mergedActivity ) );
		assertThat( activityProxy, not( CoreMatchers.sameInstance( mergedActivity ) ) );

		inTransaction(
				session -> {
					final Instruction instruction = session.get( Instruction.class, 0 );
					assertThat( instruction.getSummary(), is( "Instruction #0" ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11147")
	public void testMergeDetachInitializedByAccessProxy() {
		final Activity activityProxy = fromTransaction(
				session -> {
					final Activity activity = session.load( Activity.class, 0 );
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

		final Activity mergedActivity = fromTransaction(
				session -> (Activity) session.merge( activityProxy )
		);

		assertTrue( Hibernate.isInitialized( mergedActivity ) );
		assertThat( activityProxy, not( CoreMatchers.sameInstance( mergedActivity ) ) );

		inTransaction(
				session -> {
					final Instruction instruction = session.get( Instruction.class, 0 );
					assertThat( instruction.getSummary(), is( "Instruction #0" ) );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
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
		sources.addAnnotatedClass( Activity.class );
		sources.addAnnotatedClass( Instruction.class );
		sources.addAnnotatedClass( WebApplication.class );
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					// create a slew of Activity objects, some with Instruction reference
					// some without.

					for ( int i = 0; i < 30; i++ ) {
						final Activity activity = new Activity( i, "Activity #" + i, null );
						if ( i % 2 == 0 ) {
							final Instruction instr = new Instruction( i, "Instruction #" + i );
							activity.setInstruction( instr );
							session.save( instr );
						}
						else {
							final WebApplication webApplication = new WebApplication( i, "http://" + i + ".com" );
							webApplication.setName( "name #" + i );
							activity.setWebApplication( webApplication );
							webApplication.getActivities().add( activity );
							session.save( webApplication );
						}

						session.save( activity );
					}
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Activity" ).executeUpdate();
					session.createQuery( "delete from Instruction" ).executeUpdate();
					session.createQuery( "delete from WebApplication" ).executeUpdate();
				}
		);
	}
}