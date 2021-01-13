/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone;

import org.hibernate.Hibernate;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Same as {@link InstrumentedLazyToOneTest} except here we enable bytecode-enhanced proxies
 */
@RunWith( BytecodeEnhancerRunner.class )
public class InstrumentedProxyLazyToOneTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Airport.class, Flight.class };
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Override
	protected void prepareTest() throws Exception {
		inTransaction(
				(session) -> {
					final Airport austin = new Airport( 1, "AUS" );
					final Airport baltimore = new Airport( 2, "BWI" );

					final Flight flight1 = new Flight( 1, "ABC-123", austin, baltimore );
					final Flight flight2 = new Flight( 2, "ABC-987", baltimore, austin );

					session.persist( austin );
					session.persist( baltimore );

					session.persist( flight1 );
					session.persist( flight2 );
				}
		);
	}

	@Override
	protected void cleanupTestData() throws Exception {
		inTransaction(
				(session) -> {
					session.createQuery( "delete Flight" ).executeUpdate();
					session.createQuery( "delete Airport" ).executeUpdate();
				}
		);
	}

	@Test
	public void testEnhancedWithProxy() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		inTransaction(
				(session) -> {
					final Flight flight1 = session.byId( Flight.class ).load( 1 );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( Hibernate.isInitialized( flight1 ), is( true ) );

					assertThat( Hibernate.isPropertyInitialized( flight1, "origination" ), is( true ) );
					assertThat( Hibernate.isInitialized( flight1.getOrigination() ), is( false ) );
					// let's make sure these `Hibernate` calls pass for the right reasons...
					assertThat( flight1.getOrigination(), instanceOf( PersistentAttributeInterceptable.class ) );
					final PersistentAttributeInterceptable originationProxy = (PersistentAttributeInterceptable) flight1.getOrigination();
					assertThat( originationProxy.$$_hibernate_getInterceptor(), notNullValue() );
					assertThat( originationProxy.$$_hibernate_getInterceptor(), instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					assertThat( Hibernate.isPropertyInitialized( flight1, "destination" ), is( true ) );
					assertThat( Hibernate.isInitialized( flight1.getDestination() ), is( false ) );
					// let's make sure these `Hibernate` calls pass for the right reasons...
					assertThat( flight1.getDestination(), instanceOf( PersistentAttributeInterceptable.class ) );
					final PersistentAttributeInterceptable destinationProxy = (PersistentAttributeInterceptable) flight1.getDestination();
					assertThat( destinationProxy.$$_hibernate_getInterceptor(), notNullValue() );
					assertThat( destinationProxy.$$_hibernate_getInterceptor(), instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );
				}
		);
	}

}
