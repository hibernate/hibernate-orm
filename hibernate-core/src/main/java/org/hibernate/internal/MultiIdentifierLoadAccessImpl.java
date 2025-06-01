/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
class MultiIdentifierLoadAccessImpl<T> implements MultiIdentifierLoadAccess<T>, MultiIdLoadOptions {
	private final SharedSessionContractImplementor session;
	private final EntityPersister entityPersister;

	private LockOptions lockOptions;
	private CacheMode cacheMode;
	private Boolean readOnly;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	private Integer batchSize;
	private boolean sessionCheckingEnabled;
	private boolean returnOfDeletedEntitiesEnabled;
	private boolean orderedReturnEnabled = true;

	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	public MultiIdentifierLoadAccessImpl(SharedSessionContractImplementor session, EntityPersister entityPersister) {
		this.session = session;
		this.entityPersister = entityPersister;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public final MultiIdentifierLoadAccess<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> withReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Override
	public Integer getBatchSize() {
		return batchSize;
	}

	@Override
	public MultiIdentifierLoadAccess<T> withBatchSize(int batchSize) {
		this.batchSize = batchSize < 1 ? null : batchSize;
		return this;
	}

	@Override
	public boolean isSessionCheckingEnabled() {
		return sessionCheckingEnabled;
	}

	@Override
	public boolean isSecondLevelCacheCheckingEnabled() {
		return cacheMode == CacheMode.NORMAL || cacheMode == CacheMode.GET;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled) {
		this.sessionCheckingEnabled = enabled;
		return this;
	}

	@Override
	public boolean isReturnOfDeletedEntitiesEnabled() {
		return returnOfDeletedEntitiesEnabled;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled) {
		this.returnOfDeletedEntitiesEnabled = enabled;
		return this;
	}

	@Override
	public boolean isOrderReturnEnabled() {
		return orderedReturnEnabled;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableOrderedReturn(boolean enabled) {
		this.orderedReturnEnabled = enabled;
		return this;
	}

	@Override
	public Boolean getReadOnly(SessionImplementor session) {
		return readOnly != null
				? readOnly
				: session.getLoadQueryInfluencers().getReadOnly();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <K> List<T> multiLoad(K... ids) {
		return perform( () -> (List<T>) entityPersister.multiLoad( ids, session, this ) );
	}

	public List<T> perform(Supplier<List<T>> executor) {
		final CacheMode sessionCacheMode = session.getCacheMode();
		boolean cacheModeChanged = false;
		if ( cacheMode != null ) {
			// naive check for now...
			// todo : account for "conceptually equal"
			if ( cacheMode != sessionCacheMode ) {
				session.setCacheMode( cacheMode );
				cacheModeChanged = true;
			}
		}

		try {
			final LoadQueryInfluencers influencers = session.getLoadQueryInfluencers();
			final HashSet<String> fetchProfiles =
					influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
			final EffectiveEntityGraph effectiveEntityGraph =
					influencers.applyEntityGraph( rootGraph, graphSemantic );
			try {
				return executor.get();
			}
			finally {
				effectiveEntityGraph.clear();
				influencers.setEnabledFetchProfileNames( fetchProfiles );
			}
		}
		finally {
			if ( cacheModeChanged ) {
				// change it back
				session.setCacheMode( sessionCacheMode );
			}
		}
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <K> List<T> multiLoad(List<K> ids) {
		return ids.isEmpty()
				? emptyList()
				: perform( () -> (List<T>) entityPersister.multiLoad( ids.toArray(), session, this ) );
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableFetchProfile(String profileName) {
		if ( !session.getFactory().containsFetchProfileDefinition( profileName ) ) {
			throw new UnknownProfileException( profileName );
		}
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
		if ( disabledFetchProfiles != null ) {
			disabledFetchProfiles.remove( profileName );
		}
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> disableFetchProfile(String profileName) {
		if ( disabledFetchProfiles == null ) {
			disabledFetchProfiles = new HashSet<>();
		}
		disabledFetchProfiles.add( profileName );
		if ( enabledFetchProfiles != null ) {
			enabledFetchProfiles.remove( profileName );
		}
		return this;
	}
}
