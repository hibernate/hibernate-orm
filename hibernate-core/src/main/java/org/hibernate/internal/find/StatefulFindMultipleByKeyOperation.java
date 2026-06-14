/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FindMultipleOption;
import org.hibernate.KeyType;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.ReadOnlyMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/// Support for loading multiple entities (of a type) by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL]).
///
/// @see org.hibernate.Session#findMultiple
/// @see KeyType
///
/// @author Steve Ebersole
public class StatefulFindMultipleByKeyOperation<T> extends AbstractFindMultipleByKeyOperation<T> {
	private final StatefulLoadAccessContext loadAccessContext;

	public StatefulFindMultipleByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nonnull StatefulLoadAccessContext loadAccessContext,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@Nonnull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		super( entityDescriptor,
				defaultLockOptions, defaultCacheMode, defaultReadOnly,
				sessionFactory, findOptions );
		this.loadAccessContext = loadAccessContext;
	}

	@Override
	protected SessionImplementor getSession() {
		return loadAccessContext.getSession();
	}

	@Override
	protected List<T> withOptions(
			SharedSessionContractImplementor sharedSession,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action) {
		final var session = getSession();
		final var readOnly = session.isDefaultReadOnly();
		final var cacheMode = session.getCacheMode();
		try {
			return withLoadQueryInfluencers( session, graphSemantic, rootGraph, () -> {
				session.setDefaultReadOnly( getReadOnlyMode() == ReadOnlyMode.READ_ONLY );
				session.setCacheMode( getCacheMode() );
				try {
					return action.get();
				}
				finally {
					loadAccessContext.delayedAfterCompletion();
				}
			} );
		}
		finally {
			session.setDefaultReadOnly( readOnly );
			session.setCacheMode( cacheMode );
		}
	}

//	private Object getCachedNaturalIdResolution(
//			Object normalizedNaturalIdValue,
//			StatefulLoadAccessContext loadAccessContext) {
//		loadAccessContext.checkOpenOrWaitingForAutoClose();
//		loadAccessContext.pulseTransactionCoordinator();
//
//		return loadAccessContext
//				.getSession()
//				.getPersistenceContextInternal()
//				.getNaturalIdResolutions()
//				.findCachedIdByNaturalId( normalizedNaturalIdValue, getEntityDescriptor() );
//	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Temporarily defined full constructor in support of
	/// [org.hibernate.MultiIdentifierLoadAccess] and [org.hibernate.MultiIdentifierLoadAccess].
	///
	/// @deprecated [org.hibernate.MultiIdentifierLoadAccess] and [org.hibernate.MultiIdentifierLoadAccess]
	/// are both also deprecated.
	@Deprecated
	public StatefulFindMultipleByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nonnull StatefulLoadAccessContext loadAccessContext,
			KeyType keyType,
			FindMultipleOption.BatchSize batchSize,
			FindMultipleOption.SessionCheckMode sessionCheckMode,
			FindMultipleOption.RemovalsMode removalsMode,
			FindMultipleOption.OrderingMode orderingMode,
			CacheMode cacheMode,
			LockOptions lockOptions,
			ReadOnlyMode readOnlyMode,
			Set<String> enabledFetchProfiles,
			Set<String> disabledFetchProfiles,
			NaturalIdSynchronization naturalIdSynchronization) {
		super(
				entityDescriptor,
				keyType,
				batchSize,
				sessionCheckMode,
				removalsMode,
				orderingMode,
				cacheMode,
				lockOptions,
				readOnlyMode,
				enabledFetchProfiles,
				disabledFetchProfiles,
				naturalIdSynchronization
		);
		this.loadAccessContext = loadAccessContext;
	}
}
