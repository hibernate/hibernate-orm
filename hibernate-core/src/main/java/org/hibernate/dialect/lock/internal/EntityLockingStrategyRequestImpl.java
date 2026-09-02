/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import java.util.Objects;

import jakarta.persistence.PessimisticLockScope;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.spi.EntityLockTarget;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyKind;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyRequest;
import org.hibernate.dialect.lock.spi.LockingStrategy;
import org.hibernate.persister.entity.EntityPersister;

/// Hibernate-owned immutable entity-locking request.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class EntityLockingStrategyRequestImpl implements EntityLockingStrategyRequest {
	private final EntityPersister persister;
	private final EntityLockTarget target;
	private final LockMode lockMode;
	private final PessimisticLockScope lockScope;

	public EntityLockingStrategyRequestImpl(
			EntityPersister persister,
			LockMode lockMode,
			PessimisticLockScope lockScope) {
		this.persister = Objects.requireNonNull( persister, "Entity locking target persister must not be null" );
		this.target = new Target( persister.getEntityName(), persister.isVersioned() );
		this.lockMode = Objects.requireNonNull( lockMode, "Entity locking mode must not be null" );
		this.lockScope = Objects.requireNonNull( lockScope, "Entity locking scope must not be null" );
	}

	@Override
	public EntityLockTarget target() {
		return target;
	}

	@Override
	public LockMode lockMode() {
		return lockMode;
	}

	@Override
	public PessimisticLockScope lockScope() {
		return lockScope;
	}

	@Override
	public LockingStrategy createStrategy(EntityLockingStrategyKind kind) {
		return switch ( Objects.requireNonNull( kind, "Entity locking strategy kind must not be null" ) ) {
			case STANDARD -> createStandardStrategy();
			case SQL_AST -> createSqlAstStrategy();
			case SELECT -> createSelectStrategy();
			case UPDATE -> createUpdateStrategy();
		};
	}

	private LockingStrategy createStandardStrategy() {
		return switch ( lockMode ) {
			case PESSIMISTIC_READ, PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED -> createSqlAstStrategy();
			case PESSIMISTIC_FORCE_INCREMENT -> new PessimisticForceIncrementLockingStrategy( persister, lockMode );
			case OPTIMISTIC_FORCE_INCREMENT -> new OptimisticForceIncrementLockingStrategy( persister, lockMode );
			case OPTIMISTIC -> new OptimisticLockingStrategy( persister, lockMode );
			case READ -> new SelectLockingStrategy( persister, lockMode );
			default -> throw unsupportedKind( EntityLockingStrategyKind.STANDARD );
		};
	}

	private LockingStrategy createSqlAstStrategy() {
		return switch ( lockMode ) {
			case PESSIMISTIC_READ, PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED ->
					new SqlAstBasedLockingStrategy( persister, lockMode, lockScope );
			default -> throw unsupportedKind( EntityLockingStrategyKind.SQL_AST );
		};
	}

	private LockingStrategy createSelectStrategy() {
		return switch ( lockMode ) {
			case READ -> new SelectLockingStrategy( persister, lockMode );
			case PESSIMISTIC_READ -> new PessimisticReadSelectLockingStrategy( persister, lockMode );
			case PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED ->
					new PessimisticWriteSelectLockingStrategy( persister, lockMode );
			default -> throw unsupportedKind( EntityLockingStrategyKind.SELECT );
		};
	}

	private LockingStrategy createUpdateStrategy() {
		if ( !target.versioned() ) {
			throw new IllegalArgumentException(
					"Entity '" + target.entityName() + "' has no version and may not be locked via an update statement"
			);
		}
		return switch ( lockMode ) {
			case PESSIMISTIC_READ -> new PessimisticReadUpdateLockingStrategy( persister, lockMode );
			case PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED ->
					new PessimisticWriteUpdateLockingStrategy( persister, lockMode );
			default -> throw unsupportedKind( EntityLockingStrategyKind.UPDATE );
		};
	}

	private IllegalArgumentException unsupportedKind(EntityLockingStrategyKind kind) {
		return new IllegalArgumentException( "Lock mode " + lockMode + " is not supported by " + kind + " entity locking" );
	}

	private record Target(String entityName, boolean versioned) implements EntityLockTarget {
	}
}
