/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyKind;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyRequest;
import org.hibernate.dialect.lock.spi.LockingStrategy;
import org.hibernate.dialect.lock.spi.LockingStrategyException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Example provider-owned entity-locking factory which combines stock
/// Hibernate strategies without depending on mapping-model or internal types.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ExampleEntityLockingStrategyFactory implements EntityLockingStrategyFactory {
	public static final ExampleEntityLockingStrategyFactory INSTANCE = new ExampleEntityLockingStrategyFactory();

	private ExampleEntityLockingStrategyFactory() {
	}

	@Override
	public LockingStrategy createStrategy(EntityLockingStrategyRequest request) {
		final var target = request.target();
		if ( target.entityName().startsWith( "Fixture" ) ) {
			if ( supportsSqlAst( request ) && request.lockScope() == PessimisticLockScope.EXTENDED ) {
				return new ExampleLockingStrategy( request.createStrategy( EntityLockingStrategyKind.SQL_AST ) );
			}
			if ( request.lockMode() == LockMode.PESSIMISTIC_READ && target.versioned() ) {
				return request.createStrategy( EntityLockingStrategyKind.UPDATE );
			}
			return switch ( request.lockMode() ) {
				case PESSIMISTIC_WRITE ->
						new ExampleLockingStrategy( request.createStrategy( EntityLockingStrategyKind.SELECT ) );
				default -> EntityLockingStrategies.standard().createStrategy( request );
			};
		}
		return EntityLockingStrategies.standard().createStrategy( request );
	}

	private static boolean supportsSqlAst(EntityLockingStrategyRequest request) {
		return switch ( request.lockMode() ) {
			case PESSIMISTIC_READ, PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED -> true;
			default -> false;
		};
	}

	static final class ExampleLockingStrategy implements LockingStrategy {
		private final LockingStrategy delegate;

		private ExampleLockingStrategy(LockingStrategy delegate) {
			this.delegate = delegate;
		}

		@Override
		public void lock(
				Object id,
				Object version,
				Object object,
				Timeout timeout,
				SharedSessionContractImplementor session)
				throws StaleObjectStateException, LockingStrategyException {
			delegate.lock( id, version, object, timeout, session );
		}
	}
}
