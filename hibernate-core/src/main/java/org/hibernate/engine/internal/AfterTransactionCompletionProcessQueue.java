/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.BulkOperationCleanupAction.BulkOperationCleanUpAfterTransactionCompletionProcess;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;

/**
 * Encapsulates behavior needed for after transaction processing
 */
class AfterTransactionCompletionProcessQueue
		extends AbstractTransactionCompletionProcessQueue<AfterCompletionCallback> {

	private final Set<String> querySpacesToInvalidate = new HashSet<>();

	AfterTransactionCompletionProcessQueue(SharedSessionContractImplementor session) {
		super( session );
	}

	void addSpaceToInvalidate(String space) {
		querySpacesToInvalidate.add( space );
	}

	@Override
	boolean hasActions() {
		return super.hasActions() || !querySpacesToInvalidate.isEmpty();
	}

	void afterTransactionCompletion(boolean success) {
		AfterCompletionCallback process;
		while ( (process = processes.poll()) != null ) {
			callAfterCompletion( success, process );
		}
		invalidateCaches();
	}

	void executePendingBulkOperationCleanUpActions() {
		if ( performBulkOperationCallbacks() ) {
			invalidateCaches();
		}
	}

	private boolean performBulkOperationCallbacks() {
		boolean hasPendingBulkOperationCleanUpActions = false;
		var iterator = processes.iterator();
		while ( iterator.hasNext() ) {
			var process = iterator.next();
			if ( process instanceof BulkOperationCleanUpAfterTransactionCompletionProcess ) {
				hasPendingBulkOperationCleanUpActions = true;
				if ( callAfterCompletion( true, process ) ) {
					iterator.remove();
				}
			}
		}
		return hasPendingBulkOperationCleanUpActions;
	}

	private boolean callAfterCompletion(boolean success, AfterCompletionCallback process) {
		try {
			process.doAfterTransactionCompletion( success, session );
			return true;
		}
		catch (CacheException ce) {
			CORE_LOGGER.unableToReleaseCacheLock( ce );
			// continue loop
			return false;
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to perform afterTransactionCompletion callback: " + e.getMessage(), e );
		}
	}

	private void invalidateCaches() {
		final var factory = session.getFactory();
		if ( factory.getSessionFactoryOptions().isQueryCacheEnabled() ) {
			factory.getCache().getTimestampsCache().
					invalidate( querySpacesToInvalidate.toArray( EMPTY_STRING_ARRAY ), session );
		}
		querySpacesToInvalidate.clear();
	}
}
