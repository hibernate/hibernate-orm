/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class StatelessFindMultipleByKeyOperation<T> extends AbstractFindMultipleByKeyOperation<T> {

	@Nonnull
	private final StatelessLoadAccessContext loadAccessContext;

	public StatelessFindMultipleByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nonnull StatelessLoadAccessContext loadAccessContext,
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
	protected StatelessSessionImplementor getSession() {
		return loadAccessContext.getStatelessSession();
	}

	@Override
	protected List<T> withOptions(
			SharedSessionContractImplementor sharedSession,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action) {
		return withLoadQueryInfluencers( sharedSession, graphSemantic, rootGraph, action );
	}
}
