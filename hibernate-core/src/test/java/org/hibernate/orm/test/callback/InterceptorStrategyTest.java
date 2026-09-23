/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import java.util.function.Supplier;

import org.hibernate.boot.MetadataSources;
import org.hibernate.callback.internal.GlobalInterceptorStrategy;
import org.hibernate.callback.internal.NoInterceptorStrategy;
import org.hibernate.callback.internal.ProvidedInterceptorStrategy;
import org.hibernate.callback.internal.ScopedInterceptorStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.orm.test.interceptor.StatefulInterceptor;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@JiraKey("HHH-12168")
public class InterceptorStrategyTest {

	@Test
	public void testNoInterceptorStrategyReturnsSingleton() {
		assertSame(
				EmptyInterceptor.INSTANCE,
				NoInterceptorStrategy.INSTANCE.getInterceptorForSession( null )
		);
		assertNull( NoInterceptorStrategy.INSTANCE.getFactoryInterceptor() );
	}

	@Test
	public void testProvidedInterceptorStrategy() {
		final var interceptor = new StatefulInterceptor();
		final var strategy = new ProvidedInterceptorStrategy( interceptor );

		assertSame( interceptor, strategy.getInterceptorForSession( null ) );
		assertSame( interceptor, strategy.getFactoryInterceptor() );
	}

	@DomainModel(xmlMappings = "org/hibernate/orm/test/interceptor/User.hbm.xml")
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.INTERCEPTOR, value = "org.hibernate.orm.test.interceptor.StatefulInterceptor"))
	@SessionFactory
	@Test
	public void testGlobalInterceptorStrategyViaClassName(SessionFactoryScope scope) {
		final var options = scope.getSessionFactory().getSessionFactoryOptions();
		final var strategy = options.getInterceptorStrategy();

		assertInstanceOf( GlobalInterceptorStrategy.class, strategy );
		assertInstanceOf( StatefulInterceptor.class, strategy.getInterceptorForSession( scope.getSessionFactory() ) );
		assertSame( strategy.getInterceptorForSession( scope.getSessionFactory() ), strategy.getFactoryInterceptor() );
	}

	@Test
	public void testGlobalInterceptorStrategyViaClassObject() {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.INTERCEPTOR, StatefulInterceptor.class )
				.build() ) {
			try ( var sessionFactory = (SessionFactoryImplementor) new MetadataSources( services )
					.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build() ) {
				final var strategy = sessionFactory.getSessionFactoryOptions().getInterceptorStrategy();

				assertInstanceOf( GlobalInterceptorStrategy.class, strategy );
				assertInstanceOf( StatefulInterceptor.class, strategy.getInterceptorForSession( sessionFactory ) );
				assertSame(
						strategy.getInterceptorForSession( sessionFactory ),
						strategy.getFactoryInterceptor()
				);
			}
		}
	}

	@DomainModel(xmlMappings = "org/hibernate/orm/test/interceptor/User.hbm.xml")
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.SESSION_SCOPED_INTERCEPTOR, value = "org.hibernate.orm.test.interceptor.StatefulInterceptor"))
	@SessionFactory
	@Test
	public void testScopedInterceptorStrategyCreatesDistinctInstances(SessionFactoryScope scope) {
		final var options = scope.getSessionFactory().getSessionFactoryOptions();
		final var strategy = options.getInterceptorStrategy();

		assertInstanceOf( ScopedInterceptorStrategy.class, strategy );
		assertNull( strategy.getFactoryInterceptor() );

		final var interceptor1 = strategy.getInterceptorForSession( scope.getSessionFactory() );
		final var interceptor2 = strategy.getInterceptorForSession( scope.getSessionFactory() );

		assertInstanceOf( StatefulInterceptor.class, interceptor1 );
		assertInstanceOf( StatefulInterceptor.class, interceptor2 );
		assertNotSame( interceptor1, interceptor2 );

		strategy.releaseInterceptor( interceptor1 );
		strategy.releaseInterceptor( interceptor2 );
	}

	@DomainModel(xmlMappings = "org/hibernate/orm/test/interceptor/User.hbm.xml")
	@SessionFactory
	@Test
	public void testNoConfigurationDefaultsToNoInterceptorStrategy(SessionFactoryScope scope) {
		final var strategy = scope.getSessionFactory().getSessionFactoryOptions().getInterceptorStrategy();
		assertInstanceOf( NoInterceptorStrategy.class, strategy );
	}

	@DomainModel(xmlMappings = "org/hibernate/orm/test/interceptor/User.hbm.xml")
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.SESSION_SCOPED_INTERCEPTOR, value = "org.hibernate.orm.test.interceptor.StatefulInterceptor"))
	@SessionFactory
	@Test
	public void testStrategyInterceptorUsedInSession(SessionFactoryScope scope) {
		scope.inTransaction(
				factory -> factory.withOptions().openSession(),
				session -> {
					final var interceptor = session.getInterceptor();
					assertInstanceOf( StatefulInterceptor.class, interceptor );
				}
		);
	}

	@Test
	public void testSupplierFallbackReturnsNoInterceptorStrategy() {
		final Supplier<StatefulInterceptor> supplier = StatefulInterceptor::new;
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.SESSION_SCOPED_INTERCEPTOR, supplier )
				.build() ) {
			try ( var sessionFactory = (SessionFactoryImplementor) new MetadataSources( services )
					.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build() ) {
				final var strategy = sessionFactory.getSessionFactoryOptions().getInterceptorStrategy();
				assertInstanceOf( NoInterceptorStrategy.class, strategy );

				final var supplierResult = sessionFactory.getSessionFactoryOptions()
						.getStatelessInterceptorImplementorSupplier();
				assertNotNull( supplierResult );
			}
		}
	}

	@Test
	public void testExplicitSessionInterceptorTakesPrecedenceOverStrategy() {
		final var explicitInterceptor = new StatefulInterceptor();
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.SESSION_SCOPED_INTERCEPTOR,
						StatefulInterceptor.class.getName() )
				.build() ) {
			try ( var sessionFactory = (SessionFactoryImplementor) new MetadataSources( services )
					.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build() ) {
				assertInstanceOf( ScopedInterceptorStrategy.class,
						sessionFactory.getSessionFactoryOptions().getInterceptorStrategy() );

				try ( var session = sessionFactory.withOptions()
						.interceptor( explicitInterceptor )
						.openSession() ) {
					assertSame( explicitInterceptor, session.getInterceptor() );
				}
			}
		}
	}

	@Test
	public void testNonCdiBeanContainerWithGlobalStrategy() {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.INTERCEPTOR, StatefulInterceptor.class.getName() )
				.build() ) {
			final var registry = services.requireService( ManagedBeanRegistry.class );
			final var strategy = new GlobalInterceptorStrategy( StatefulInterceptor.class, services );

			final var interceptor = strategy.getInterceptorForSession( null );
			assertInstanceOf( StatefulInterceptor.class, interceptor );
			assertSame( interceptor, strategy.getFactoryInterceptor() );
			assertSame( interceptor, strategy.getInterceptorForSession( null ) );
		}
	}

	@Test
	public void testNonCdiBeanContainerWithScopedStrategy() {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class.getName() )
				.build() ) {
			final var registry = services.requireService( ManagedBeanRegistry.class );
			final var strategy = new ScopedInterceptorStrategy( StatefulInterceptor.class, services );

			final var first = strategy.getInterceptorForSession( null );
			final var second = strategy.getInterceptorForSession( null );

			assertInstanceOf( StatefulInterceptor.class, first );
			assertInstanceOf( StatefulInterceptor.class, second );
			assertNotSame( first, second );

			strategy.releaseInterceptor( first );
			strategy.releaseInterceptor( second );
		}
	}

	@Test
	public void testNoSessionInterceptorCreationPreventsStrategyInterceptor() {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.SESSION_SCOPED_INTERCEPTOR,
						StatefulInterceptor.class.getName() )
				.build() ) {
			try ( var sessionFactory = (SessionFactoryImplementor) new MetadataSources( services )
					.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build() ) {
				assertInstanceOf( ScopedInterceptorStrategy.class,
						sessionFactory.getSessionFactoryOptions().getInterceptorStrategy() );

				try ( var session = sessionFactory.withOptions()
						.noSessionInterceptorCreation()
						.openSession() ) {
					final var interceptor = session.getInterceptor();
					assertSame( EmptyInterceptor.INSTANCE, interceptor );
				}
			}
		}
	}
}
