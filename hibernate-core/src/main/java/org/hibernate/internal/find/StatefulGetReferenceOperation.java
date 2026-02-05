/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Incubating;
import org.hibernate.KeyType;
import org.hibernate.LockOptions;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.spi.NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;
import static org.hibernate.internal.find.Helper.coerceId;

/// GetReferenceOperation implementation for a [stateful session][org.hibernate.Session].
///
/// @author Steve Ebersole
@Incubating
public class StatefulGetReferenceOperation<T> implements GetReferenceOperation<T> {
	private final EntityPersister entityDescriptor;
	private final StatefulLoadAccessContext loadAccessContext;

	private final KeyType keyType;

	public StatefulGetReferenceOperation(
			@NonNull EntityPersister entityDescriptor,
			@NonNull StatefulLoadAccessContext loadAccessContext,
			KeyType keyType) {
		this.entityDescriptor = entityDescriptor;
		this.loadAccessContext = loadAccessContext;
		this.keyType = keyType;
	}

	@Override
	public T performGetReference(Object key) {
		loadAccessContext.checkOpenOrWaitingForAutoClose();
		loadAccessContext.pulseTransactionCoordinator();

		if ( keyType == KeyType.NATURAL ) {
			return getReferenceByNaturalId( key );
		}
		else {
			return getReferenceById( key );
		}
	}

	private T getReferenceByNaturalId(Object key) {
		final var normalizedKey = Helper.coerceNaturalId( entityDescriptor, key );

		final var session = loadAccessContext.getSession();
		final var naturalIdResolutions = session.getPersistenceContextInternal().getNaturalIdResolutions();

		final Object cachedResolution = naturalIdResolutions.findCachedIdByNaturalId( normalizedKey, entityDescriptor );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}

		if ( cachedResolution != null ) {
			return getReferenceById( cachedResolution );
		}

		final var loadedResolution = entityDescriptor.getNaturalIdLoader().resolveNaturalIdToId( normalizedKey, session );
		naturalIdResolutions.cacheResolutionFromLoad( loadedResolution, normalizedKey, entityDescriptor );
		return getReferenceById( loadedResolution );
	}

	private T getReferenceById(Object key) {
		final Object normalizedId = coerceId( entityDescriptor, key, loadAccessContext.getSession().getFactory() );
		final var concreteType = entityDescriptor.resolveConcreteProxyTypeForId( normalizedId, loadAccessContext.getSession() );

		//noinspection unchecked,removal
		return (T) loadAccessContext.load(
				LoadEventListener.LOAD,
				normalizedId,
				concreteType.getEntityName(),
				LockOptions.NONE,
				false
		);
	}
}
