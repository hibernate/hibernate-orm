/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PessimisticLockScope;

import org.hibernate.LockMode;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.EntityLockTarget;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyKind;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyRequest;
import org.hibernate.dialect.lock.spi.LockingStrategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies community Dialect use of the entity-locking factory SPI.
///
/// @author Steve Ebersole
public class EntityLockingStrategyFactoryTest {
	@Test
	void updateBasedDialectsUseTheStockUpdateProfile() {
		assertThat( new CacheDialect().getEntityLockingStrategyFactory() )
				.isSameAs( EntityLockingStrategies.pessimisticUpdate() );
		assertThat( new RDMSOS2200Dialect().getEntityLockingStrategyFactory() )
				.isSameAs( EntityLockingStrategies.pessimisticUpdate() );
		assertThat( new TimesTenDialect().getEntityLockingStrategyFactory() )
				.isSameAs( EntityLockingStrategies.pessimisticUpdate() );
	}

	@Test
	void hsqlLegacyUsesTheSameStandardProfileAcrossVersionBranches() {
		assertThat( new HSQLLegacyDialect( DatabaseVersion.make( 1 ) ).getEntityLockingStrategyFactory() )
				.isSameAs( EntityLockingStrategies.standard() );
		assertThat( new HSQLLegacyDialect( DatabaseVersion.make( 2 ) ).getEntityLockingStrategyFactory() )
				.isSameAs( EntityLockingStrategies.standard() );
	}

	@Test
	void irisSelectsByModeAndVersionedTarget() {
		final EntityLockingStrategyFactory factory = new InterSystemsIRISDialect().getEntityLockingStrategyFactory();

		assertKinds( factory, true, LockMode.PESSIMISTIC_READ, EntityLockingStrategyKind.UPDATE );
		assertKinds( factory, false, LockMode.PESSIMISTIC_READ, EntityLockingStrategyKind.SELECT );
		assertKinds( factory, true, LockMode.PESSIMISTIC_WRITE, EntityLockingStrategyKind.UPDATE );
		assertKinds( factory, false, LockMode.PESSIMISTIC_WRITE, EntityLockingStrategyKind.SELECT );
		assertKinds( factory, true, LockMode.OPTIMISTIC, EntityLockingStrategyKind.STANDARD );
	}

	private static void assertKinds(
			EntityLockingStrategyFactory factory,
			boolean versioned,
			LockMode lockMode,
			EntityLockingStrategyKind expectedKind) {
		final List<EntityLockingStrategyKind> kinds = new ArrayList<>();

		factory.createStrategy( request( versioned, lockMode, kinds ) );

		assertThat( kinds ).containsExactly( expectedKind );
	}

	private static EntityLockingStrategyRequest request(
			boolean versioned,
			LockMode lockMode,
			List<EntityLockingStrategyKind> kinds) {
		final EntityLockTarget target = proxy(
				EntityLockTarget.class,
				(method, arguments) -> switch ( method.getName() ) {
					case "entityName" -> "Book";
					case "versioned" -> versioned;
					default -> defaultValue( method.getReturnType() );
				}
		);
		return proxy(
				EntityLockingStrategyRequest.class,
				(method, arguments) -> switch ( method.getName() ) {
					case "target" -> target;
					case "lockMode" -> lockMode;
					case "lockScope" -> PessimisticLockScope.NORMAL;
					case "createStrategy" -> {
						kinds.add( (EntityLockingStrategyKind) arguments[0] );
						yield (LockingStrategy) (id, version, object, timeout, session) -> {
						};
					}
					default -> defaultValue( method.getReturnType() );
				}
		);
	}

	private static <T> T proxy(Class<T> contract, Invocation invocation) {
		return contract.cast( Proxy.newProxyInstance(
				EntityLockingStrategyFactoryTest.class.getClassLoader(),
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
