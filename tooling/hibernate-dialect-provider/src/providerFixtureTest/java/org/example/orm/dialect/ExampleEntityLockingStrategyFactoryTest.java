/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.spi.EntityLockTarget;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyKind;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyRequest;
import org.hibernate.dialect.lock.spi.LockingStrategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that an external Dialect can inspect an entity-locking request,
/// select stock behavior, and wrap a stock strategy using only public SPI.
///
/// @author Steve Ebersole
public class ExampleEntityLockingStrategyFactoryTest {
	@Test
	void selectsUpdateForVersionedFixtureReadLock() {
		final List<EntityLockingStrategyKind> kinds = new ArrayList<>();

		ExampleEntityLockingStrategyFactory.INSTANCE.createStrategy(
				request( "FixtureEntity", true, LockMode.PESSIMISTIC_READ, PessimisticLockScope.NORMAL, kinds, null )
		);

		assertEquals( List.of( EntityLockingStrategyKind.UPDATE ), kinds );
	}

	@Test
	void wrapsSelectForNormalFixtureWriteLock() {
		final List<EntityLockingStrategyKind> kinds = new ArrayList<>();
		final AtomicBoolean invoked = new AtomicBoolean();

		final LockingStrategy strategy = ExampleEntityLockingStrategyFactory.INSTANCE.createStrategy(
				request( "FixtureEntity", false, LockMode.PESSIMISTIC_WRITE, PessimisticLockScope.NORMAL, kinds, invoked )
		);
		strategy.lock( 1, null, new Object(), Timeout.milliseconds( 10 ), null );

		assertInstanceOf( ExampleEntityLockingStrategyFactory.ExampleLockingStrategy.class, strategy );
		assertEquals( List.of( EntityLockingStrategyKind.SELECT ), kinds );
		assertTrue( invoked.get() );
	}

	@Test
	void wrapsSqlAstForExtendedFixtureLock() {
		final List<EntityLockingStrategyKind> kinds = new ArrayList<>();

		final LockingStrategy strategy = ExampleEntityLockingStrategyFactory.INSTANCE.createStrategy(
				request( "FixtureEntity", true, LockMode.PESSIMISTIC_READ, PessimisticLockScope.EXTENDED, kinds, null )
		);

		assertInstanceOf( ExampleEntityLockingStrategyFactory.ExampleLockingStrategy.class, strategy );
		assertEquals( List.of( EntityLockingStrategyKind.SQL_AST ), kinds );
	}

	private static EntityLockingStrategyRequest request(
			String entityName,
			boolean versioned,
			LockMode lockMode,
			PessimisticLockScope lockScope,
			List<EntityLockingStrategyKind> kinds,
			AtomicBoolean invoked) {
		final EntityLockTarget target = proxy(
				EntityLockTarget.class,
				(method, arguments) -> switch ( method.getName() ) {
					case "entityName" -> entityName;
					case "versioned" -> versioned;
					default -> defaultValue( method.getReturnType() );
				}
		);
		return proxy(
				EntityLockingStrategyRequest.class,
				(method, arguments) -> switch ( method.getName() ) {
					case "target" -> target;
					case "lockMode" -> lockMode;
					case "lockScope" -> lockScope;
					case "createStrategy" -> {
						kinds.add( (EntityLockingStrategyKind) arguments[0] );
						yield (LockingStrategy) (id, version, object, timeout, session) -> {
							if ( invoked != null ) {
								invoked.set( true );
							}
						};
					}
					default -> defaultValue( method.getReturnType() );
				}
		);
	}

	private static <T> T proxy(Class<T> contract, Invocation invocation) {
		return contract.cast( Proxy.newProxyInstance(
				ExampleEntityLockingStrategyFactoryTest.class.getClassLoader(),
				new Class<?>[] { contract },
				(proxy, method, arguments) -> invocation.invoke( method, arguments )
		) );
	}

	private static Object defaultValue(Class<?> type) {
		return type == boolean.class ? false : null;
	}

	@FunctionalInterface
	private interface Invocation {
		Object invoke(java.lang.reflect.Method method, Object[] arguments);
	}
}
