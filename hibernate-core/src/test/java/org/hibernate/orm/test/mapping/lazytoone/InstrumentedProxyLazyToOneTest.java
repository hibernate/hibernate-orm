/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.lazytoone;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Same as {@link InstrumentedLazyToOneTest} except here we enable bytecode-enhanced proxies
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				Airport.class, Flight.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class InstrumentedProxyLazyToOneTest {

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
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

	@AfterEach
	protected void cleanupTestData(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEnhancedWithProxy(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
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
