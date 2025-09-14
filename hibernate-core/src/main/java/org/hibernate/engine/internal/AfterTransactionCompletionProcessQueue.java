/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * Encapsulates behavior needed for after transaction processing
 */
public class AfterTransactionCompletionProcessQueue
		extends AbstractTransactionCompletionProcessQueue<AfterCompletionCallback> {

	private final Set<String> querySpacesToInvalidate = new HashSet<>();

	public AfterTransactionCompletionProcessQueue(SharedSessionContractImplementor session) {
		super( session );
	}

	public void addSpaceToInvalidate(String space) {
		querySpacesToInvalidate.add( space );
	}

	@Override
	public boolean hasActions() {
		return super.hasActions() || !querySpacesToInvalidate.isEmpty();
	}

	public void afterTransactionCompletion(boolean success) {
		AfterCompletionCallback process;
		while ( (process = processes.poll()) != null ) {
			try {
				process.doAfterTransactionCompletion( success, session );
			}
			catch (CacheException ce) {
				CORE_LOGGER.unableToReleaseCacheLock( ce );
				// continue loop
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to perform afterTransactionCompletion callback: " + e.getMessage(), e );
			}
		}

		final SessionFactoryImplementor factory = session.getFactory();
		if ( factory.getSessionFactoryOptions().isQueryCacheEnabled() ) {
			factory.getCache().getTimestampsCache()
					.invalidate( querySpacesToInvalidate.toArray( new String[0] ), session );
		}
		querySpacesToInvalidate.clear();
	}
}
